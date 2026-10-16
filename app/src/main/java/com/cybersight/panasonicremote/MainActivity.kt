package com.cybersight.panasonicremote

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.cybersight.panasonicremote.ir.IrTransmitter
import com.cybersight.panasonicremote.tv.ModelCatalog
import com.cybersight.panasonicremote.tv.TvProfile
import com.cybersight.panasonicremote.tv.TvProfiles
import com.cybersight.panasonicremote.tv.TvSeriesHint

class MainActivity : Activity() {
    private lateinit var transmitter: IrTransmitter
    private lateinit var statusText: TextView
    private lateinit var detailText: TextView
    private lateinit var activeProfileText: TextView
    private lateinit var profileNoteText: TextView
    private lateinit var simpleProfileText: TextView
    private lateinit var simpleStatusText: TextView
    private lateinit var brandSpinner: Spinner
    private lateinit var modelSpinner: Spinner
    private lateinit var remoteRoot: ViewGroup
    private lateinit var simpleModeView: View
    private lateinit var advancedModeView: View
    private lateinit var simpleSetupButton: Button

    private var activeProfile: TvProfile = TvProfiles.byId(TvProfiles.PANASONIC_VIERA)!!
    private var currentSeries: List<TvSeriesHint> = emptyList()
    private var suppressModelCallback = false
    private var profileConfigured = false

    private val prefs by lazy { getSharedPreferences("remote_state", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        transmitter = IrTransmitter(this)
        statusText = findViewById(R.id.statusText)
        detailText = findViewById(R.id.detailText)
        activeProfileText = findViewById(R.id.activeProfileText)
        profileNoteText = findViewById(R.id.profileNoteText)
        simpleProfileText = findViewById(R.id.simpleProfileText)
        simpleStatusText = findViewById(R.id.simpleStatusText)
        brandSpinner = findViewById(R.id.brandSpinner)
        modelSpinner = findViewById(R.id.modelSpinner)
        remoteRoot = findViewById(R.id.remoteRoot)
        simpleModeView = findViewById(R.id.simpleModeScroll)
        advancedModeView = findViewById(R.id.advancedModeScroll)
        simpleSetupButton = findViewById(R.id.simpleSetupButton)

        val savedProfile = TvProfiles.byId(prefs.getString("active_profile", null))
        profileConfigured = savedProfile != null
        activeProfile = savedProfile ?: TvProfiles.byId(TvProfiles.PANASONIC_VIERA)!!

        setupBrandSpinner()
        wireRemoteButtons(remoteRoot)
        applyProfile(activeProfile, persist = false, announce = false)
        updateHardwareStatus()
        showSimpleMode()

        findViewById<Button>(R.id.advancedModeButton).setOnClickListener { showAdvancedMode() }
        findViewById<Button>(R.id.simpleModeButton).setOnClickListener { showSimpleMode() }
        findViewById<Button>(R.id.findTvButton).setOnClickListener { startFindWizard() }
        findViewById<Button>(R.id.testPowerButton).setOnClickListener { sendCommand("POWER") }
        simpleSetupButton.setOnClickListener { startFindWizard(ModelCatalog.AUTO_BRAND) }

        if (!profileConfigured && savedInstanceState == null && transmitter.hasEmitter) {
            remoteRoot.post { startFindWizard(ModelCatalog.AUTO_BRAND) }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (advancedModeView.visibility == View.VISIBLE) {
            showSimpleMode()
        } else {
            super.onBackPressed()
        }
    }

    private fun showSimpleMode() {
        simpleModeView.visibility = View.VISIBLE
        advancedModeView.visibility = View.GONE
    }

    private fun showAdvancedMode() {
        simpleModeView.visibility = View.GONE
        advancedModeView.visibility = View.VISIBLE
    }

    private fun setupBrandSpinner() {
        brandSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            ModelCatalog.brands
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        brandSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateModelSpinner(ModelCatalog.brands[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        val initialBrand = if (profileConfigured) activeProfile.brand else ModelCatalog.AUTO_BRAND
        val savedBrandIndex = ModelCatalog.brands.indexOf(initialBrand).coerceAtLeast(0)
        brandSpinner.setSelection(savedBrandIndex)
    }

    private fun updateModelSpinner(brand: String) {
        currentSeries = ModelCatalog.seriesFor(brand)
        val labels = if (currentSeries.isEmpty()) {
            listOf("Choose a brand, or use Find My TV")
        } else {
            listOf("Choose series / model family (optional)") + currentSeries.map { it.label }
        }

        suppressModelCallback = true
        modelSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        modelSpinner.setSelection(0)
        suppressModelCallback = false

        modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressModelCallback || position == 0 || currentSeries.isEmpty()) return
                val hint = currentSeries[position - 1]
                val profile = TvProfiles.byId(hint.profileId) ?: return
                applyProfile(profile, persist = true, announce = true)
                if (hint.note.isNotBlank()) profileNoteText.text = "${profile.notes}\n${hint.note}"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun wireRemoteButtons(view: View) {
        if (view is Button) {
            val commandName = view.tag as? String
            if (!commandName.isNullOrBlank()) {
                view.setOnClickListener { sendCommand(commandName) }
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) wireRemoteButtons(view.getChildAt(index))
        }
    }

    private fun refreshDynamicButtons(view: View) {
        if (view is Button) {
            val commandName = view.tag as? String
            if (!commandName.isNullOrBlank()) {
                val supported = activeProfile.supports(commandName)
                view.visibility = if (supported) View.VISIBLE else View.GONE
                view.isEnabled = supported && transmitter.hasEmitter && profileConfigured
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) refreshDynamicButtons(view.getChildAt(index))
        }
    }

    private fun applyProfile(profile: TvProfile, persist: Boolean, announce: Boolean) {
        activeProfile = profile
        if (persist) {
            profileConfigured = true
            prefs.edit().putString("active_profile", profile.id).apply()
        }

        if (profileConfigured) {
            activeProfileText.text = "${profile.brand} · ${profile.displayName}"
            profileNoteText.text = "${profile.familyHint}\n${profile.notes}"
        } else {
            activeProfileText.text = "Not configured"
            profileNoteText.text = "Use Find / Change TV or choose a model family below."
        }

        if (profileConfigured) {
            simpleProfileText.text = profile.brand
            simpleStatusText.text = profile.displayName
            simpleSetupButton.visibility = View.GONE
        } else {
            simpleProfileText.text = "TV Remote"
            simpleStatusText.text = "Set up your TV once to start using the remote"
            simpleSetupButton.visibility = View.VISIBLE
        }

        refreshDynamicButtons(remoteRoot)
        updateActionButtons()

        if (announce) {
            statusText.text = "Profile loaded: ${profile.displayName}"
            detailText.text = "Saved. Simple Remote will use this profile automatically."
            simpleStatusText.text = "${profile.brand} · ${profile.displayName}"
        }
    }

    private fun sendCommand(name: String): Boolean {
        if (!profileConfigured) {
            showToast("Set up your TV first")
            return false
        }

        val signal = activeProfile.signal(name)
        if (signal == null) {
            showToast("${prettyName(name)} is not available in this profile")
            return false
        }

        val result = transmitter.send(signal)
        if (result.isSuccess) {
            val carrier = result.getOrThrow()
            statusText.text = "Sent ${prettyName(name)} · ${activeProfile.brand}"
            detailText.text = "${activeProfile.displayName} · ${carrier / 1000} kHz"
            return true
        }

        statusText.text = getString(R.string.ir_error)
        detailText.text = result.exceptionOrNull()?.message ?: getString(R.string.unknown_error)
        showToast(detailText.text.toString())
        return false
    }

    private fun startFindWizard(brandOverride: String? = null) {
        if (!transmitter.hasEmitter) {
            showToast(getString(R.string.no_ir_detail))
            return
        }

        val selectedBrand = brandOverride ?: brandSpinner.selectedItem?.toString() ?: ModelCatalog.AUTO_BRAND
        val candidates = TvProfiles.forBrand(selectedBrand).filter { it.supports("POWER") }
        if (candidates.isEmpty()) {
            showToast("No test profiles available for $selectedBrand")
            return
        }

        AlertDialog.Builder(this)
            .setTitle(if (profileConfigured) "Find / change TV" else "Set up your TV")
            .setMessage(
                "Point the phone at the TV. The app will try one safe Power profile at a time. " +
                    "When the TV reacts, confirm it and the app will remember the remote automatically.\n\n" +
                    "Profiles to test: ${candidates.size}"
            )
            .setPositiveButton("START") { _, _ -> showCandidate(candidates, 0) }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showCandidate(candidates: List<TvProfile>, index: Int) {
        if (index >= candidates.size) {
            AlertDialog.Builder(this)
                .setTitle("No profile confirmed")
                .setMessage("None of the bundled IR families was confirmed. Open Advanced mode to choose a brand/model family manually, or add another IR family later.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val candidate = candidates[index]
        AlertDialog.Builder(this)
            .setTitle("Test ${index + 1} of ${candidates.size}")
            .setMessage("${candidate.brand} · ${candidate.displayName}\n\nPoint the phone at the TV, then test Power.")
            .setPositiveButton("TEST POWER") { _, _ ->
                val signal = candidate.signal("POWER")
                val sent = signal != null && transmitter.send(signal).isSuccess
                if (!sent) {
                    showToast("Could not transmit this profile")
                    showCandidate(candidates, index + 1)
                } else {
                    askDidTvRespond(candidates, index)
                }
            }
            .setNeutralButton("SKIP") { _, _ -> showCandidate(candidates, index + 1) }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun askDidTvRespond(candidates: List<TvProfile>, index: Int) {
        val candidate = candidates[index]
        AlertDialog.Builder(this)
            .setTitle("Did the TV react?")
            .setMessage("Did the TV turn on/off or otherwise respond to the Power test?")
            .setPositiveButton("YES — USE THIS") { _, _ ->
                applyProfile(candidate, persist = true, announce = true)
                val brandIndex = ModelCatalog.brands.indexOf(candidate.brand)
                if (brandIndex >= 0) brandSpinner.setSelection(brandIndex)
                statusText.text = "TV profile found"
                detailText.text = "Saved ${candidate.brand} · ${candidate.displayName}"
                showSimpleMode()
                showToast("Remote ready")
            }
            .setNegativeButton("NO — NEXT") { _, _ -> showCandidate(candidates, index + 1) }
            .setNeutralButton("TEST AGAIN") { _, _ -> showCandidate(candidates, index) }
            .show()
    }

    private fun updateHardwareStatus() {
        val enabled = transmitter.hasEmitter
        statusText.text = if (enabled) getString(R.string.ir_ready) else getString(R.string.no_ir)
        detailText.text = if (enabled) {
            "IR hardware ready · ${transmitter.supportedRangeDescription()}"
        } else {
            getString(R.string.no_ir_detail)
        }

        if (!enabled) {
            simpleStatusText.text = getString(R.string.no_ir)
        } else if (profileConfigured) {
            simpleStatusText.text = "${activeProfile.brand} · ${activeProfile.displayName}"
        }

        updateActionButtons()
        refreshDynamicButtons(remoteRoot)
    }

    private fun updateActionButtons() {
        val enabled = transmitter.hasEmitter
        findViewById<Button>(R.id.testPowerButton).isEnabled = enabled && profileConfigured
        findViewById<Button>(R.id.findTvButton).isEnabled = enabled
        simpleSetupButton.isEnabled = enabled
        simpleSetupButton.visibility = if (profileConfigured) View.GONE else View.VISIBLE
    }

    private fun prettyName(name: String): String = when (name) {
        "VOL_UP" -> "Volume +"
        "VOL_DOWN" -> "Volume -"
        "CH_UP" -> "Channel +"
        "CH_DOWN" -> "Channel -"
        "HDMI_1" -> "HDMI 1"
        "HDMI_2" -> "HDMI 2"
        "HDMI_3" -> "HDMI 3"
        else -> name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    private fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
