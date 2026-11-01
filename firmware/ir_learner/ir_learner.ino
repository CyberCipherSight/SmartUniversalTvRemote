// Receive-only IR learner. No IR emitter, network, Bluetooth, OTA, or transmit commands.
#include <Arduino.h>
#include <IRremoteESP8266.h>
#include <IRrecv.h>
#include <IRutils.h>
#include <esp_timer.h>
#include <esp_system.h>

#ifndef IR_CAPTURE_GPIO
#if CONFIG_IDF_TARGET_ESP32C3 || CONFIG_IDF_TARGET_ESP32S3 || CONFIG_IDF_TARGET_ESP32S2
#define IR_CAPTURE_GPIO 4
#else
#define IR_CAPTURE_GPIO 27
#endif
#endif

constexpr uint16_t kBufferEntries = 2048;
constexpr uint8_t kFrameTimeoutMs = 20;
constexpr uint8_t kQueueLength = 6;
constexpr uint32_t kBaud = 115200;
IRrecv receiver(IR_CAPTURE_GPIO, kBufferEntries, kFrameTimeoutMs, true);

struct Capture {
  uint64_t uptimeUs;
  uint32_t sequence;
  uint16_t rawLength;
  uint16_t bits;
  int16_t protocol;
  bool overflow;
  bool repeat;
  bool stateful;
  uint64_t value;
  uint32_t address;
  uint32_t command;
  char protocolName[48];
  char stateHex[kStateSizeMax * 2 + 1];
  uint16_t rawTicks[kBufferEntries];
};

QueueHandle_t captureQueue;
volatile uint32_t capturesSeen = 0;
volatile uint32_t queueDropped = 0;
uint32_t bootId;

// Decode from the library's saved buffer. Acquisition is rearmed before decoding.
// Do not call resume() again: doing so could clear a subsequent in-flight frame.
void captureTask(void*) {
  decode_results results;
  Capture* record = new Capture;
  receiver.setUnknownThreshold(1); // Keep short UNKNOWN captures; noise can be analyzed later.
  receiver.enableIRIn(true);      // Input pull-up avoids a floating pin before a receiver is fitted.
  while (true) {
    if (receiver.decode(&results)) {
      record->uptimeUs = esp_timer_get_time();
      record->sequence = ++capturesSeen;
      record->bits = results.bits;
      record->protocol = static_cast<int16_t>(results.decode_type);
      record->overflow = results.overflow;
      record->repeat = results.repeat;
      record->stateful = hasACState(results.decode_type);
      const String protocolName = typeToString(results.decode_type, false);
      protocolName.toCharArray(record->protocolName, sizeof(record->protocolName));
      record->stateHex[0] = '\0';
      record->value = 0; record->address = 0; record->command = 0;
      if (record->stateful) {
        const uint16_t count = min(static_cast<uint16_t>((results.bits + 7) / 8), static_cast<uint16_t>(kStateSizeMax));
        for (uint16_t i = 0; i < count; i++) sprintf(record->stateHex + i * 2, "%02X", results.state[i]);
      } else {
        record->value = results.value; record->address = results.address; record->command = results.command;
      }
      record->rawLength = min(results.rawlen, kBufferEntries);
      for (uint16_t i = 0; i < record->rawLength; i++) record->rawTicks[i] = results.rawbuf[i];
      if (xQueueSend(captureQueue, record, 0) != pdTRUE) queueDropped++;
    }
    vTaskDelay(1);
  }
}

void printHex(uint64_t value) { Serial.print('"'); Serial.print(uint64ToString(value, 16)); Serial.print('"'); }
void printIdentity(const char* event) {
  Serial.printf("{\"schema_version\":1,\"event\":\"%s\",\"firmware\":\"ir-learner-1\",\"boot_id\":\"%08lX\",\"receive_only\":true,\"gpio\":%u,\"baud\":%lu,\"decoder\":\"IRremoteESP8266-2.9.0\",\"buffer_entries\":%u,\"frame_timeout_ms\":%u,\"uptime_us\":",
    event, static_cast<unsigned long>(bootId), IR_CAPTURE_GPIO, static_cast<unsigned long>(kBaud), kBufferEntries, kFrameTimeoutMs);
  Serial.print(uint64ToString(esp_timer_get_time()));
  Serial.printf(",\"captures_seen\":%lu,\"queue_dropped_total\":%lu,\"input_level\":%d,\"receiver_connection_verified\":false}\n",
    static_cast<unsigned long>(capturesSeen), static_cast<unsigned long>(queueDropped), digitalRead(IR_CAPTURE_GPIO));
}

void printCapture(const Capture& r) {
  Serial.printf("{\"schema_version\":1,\"event\":\"capture\",\"boot_id\":\"%08lX\",\"sequence\":%lu,\"uptime_us\":",
    static_cast<unsigned long>(bootId), static_cast<unsigned long>(r.sequence));
  Serial.print(uint64ToString(r.uptimeUs));
  Serial.printf(",\"protocol\":\"%s\",\"protocol_id\":%d,\"bits\":%u,\"repeat\":%s,\"overflow\":%s,\"value_hex\":",
    r.protocolName, r.protocol, r.bits, r.repeat ? "true" : "false", r.overflow ? "true" : "false");
  if (!r.stateful && r.protocol != UNKNOWN) printHex(r.value); else Serial.print("null");
  Serial.print(",\"address_hex\":"); if (!r.stateful && r.protocol != UNKNOWN) printHex(r.address); else Serial.print("null");
  Serial.print(",\"command_hex\":"); if (!r.stateful && r.protocol != UNKNOWN) printHex(r.command); else Serial.print("null");
  Serial.print(",\"unknown_hash_hex\":"); if (r.protocol == UNKNOWN) printHex(r.value); else Serial.print("null");
  Serial.print(",\"state_hex\":"); if (r.stateful) { Serial.print('"'); Serial.print(r.stateHex); Serial.print('"'); } else Serial.print("null");
  Serial.printf(",\"raw_tick_us\":%u,\"leading_gap_us\":%lu,\"raw_us\":[", kRawTick,
    r.rawLength ? static_cast<unsigned long>(r.rawTicks[0]) * kRawTick : 0UL);
  for (uint16_t i = 1; i < r.rawLength; i++) {
    if (i > 1) Serial.print(',');
    Serial.print(static_cast<uint32_t>(r.rawTicks[i]) * kRawTick);
  }
  Serial.printf("],\"receiver_center_hz\":38000,\"carrier_hz_measured\":null,\"timings_compensated\":false,\"queue_dropped_total\":%lu}\n",
    static_cast<unsigned long>(queueDropped));
}

void setup() {
  Serial.begin(kBaud);
  bootId = esp_random();
  captureQueue = xQueueCreate(kQueueLength, sizeof(Capture));
  if (!captureQueue) {
    Serial.println("{\"schema_version\":1,\"event\":\"fatal\",\"reason\":\"capture_queue_allocation\"}");
    while (true) delay(1000);
  }
  if (xTaskCreate(captureTask, "ir-receive", 8192, nullptr, 2, nullptr) != pdPASS) {
    Serial.println("{\"schema_version\":1,\"event\":\"fatal\",\"reason\":\"capture_task_allocation\"}");
    while (true) delay(1000);
  }
  printIdentity("ready");
}

void loop() {
  static Capture* output = new Capture;
  static uint32_t lastHeartbeat = 0;
  if (xQueueReceive(captureQueue, output, pdMS_TO_TICKS(10)) == pdTRUE) printCapture(*output);
  if (millis() - lastHeartbeat >= 3000) {
    printIdentity("heartbeat");
    lastHeartbeat = millis();
  }
}
