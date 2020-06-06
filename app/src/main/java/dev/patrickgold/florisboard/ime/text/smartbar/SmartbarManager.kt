package dev.patrickgold.florisboard.ime.text.smartbar

import android.content.Context
import android.view.View
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextServicesManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ToggleButton
import androidx.core.view.children
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.text.TextInputManager
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode

// TODO: Implement suggestion creation functionality
class SmartbarManager(
    private val florisboard: FlorisBoard,
    private val textInputManager: TextInputManager
) : SpellCheckerSession.SpellCheckerSessionListener {

    private val candidateViewList: MutableList<Button> = mutableListOf()
    private var candidatesView: LinearLayout? = null
    private var isComposingEnabled: Boolean = false
    private var numberRowView: LinearLayout? = null
    private var quickActionsView: LinearLayout? = null
    private var quickActionToggle: ToggleButton? = null
    private var quickActionTogglePlaceholder: View? = null
    private var smartbarView: LinearLayout? = null
    private var spellCheckerSession: SpellCheckerSession? = null

    var activeContainerId: Int = R.id.candidates
        set(value) { field = value; updateActiveContainerVisibility() }

    private val candidateViewOnClickListener = View.OnClickListener { v ->
        val view = v as Button
        val text = view.text.toString()
        if (text.isNotEmpty()) {
            textInputManager.commitCandidate(text)
        }
    }
    private val candidateViewOnLongClickListener = View.OnLongClickListener { v ->
        true
    }
    private val numberRowButtonOnClickListener = View.OnClickListener { v ->
        val keyData = when (v.id) {
            R.id.number_row_0 -> KeyData(48, "0")
            R.id.number_row_1 -> KeyData(49, "1")
            R.id.number_row_2 -> KeyData(50, "2")
            R.id.number_row_3 -> KeyData(51, "3")
            R.id.number_row_4 -> KeyData(52, "4")
            R.id.number_row_5 -> KeyData(53, "5")
            R.id.number_row_6 -> KeyData(54, "6")
            R.id.number_row_7 -> KeyData(55, "7")
            R.id.number_row_8 -> KeyData(56, "8")
            R.id.number_row_9 -> KeyData(57, "9")
            else -> KeyData(0)
        }
        florisboard.textInputManager.sendKeyPress(keyData)
    }
    private val quickActionOnClickListener = View.OnClickListener { v ->
        when (v.id) {
            R.id.quick_action_switch_to_media_context -> {
                florisboard.setActiveInput(R.id.media_input)
            }
            R.id.quick_action_open_settings -> florisboard.launchSettings()
            R.id.quick_action_one_handed_toggle -> {
                when (florisboard.prefs.looknfeel.oneHandedMode) {
                    "off" -> {
                        florisboard.prefs.looknfeel.oneHandedMode = "end"
                    }
                    else -> {
                        florisboard.prefs.looknfeel.oneHandedMode = "off"
                    }
                }
                florisboard.updateOneHandedPanelVisibility()
            }
            else -> return@OnClickListener
        }
    }
    private val quickActionToggleOnClickListener = View.OnClickListener {
        activeContainerId = when (activeContainerId) {
            R.id.quick_actions -> getPreferredContainerId()
            else -> R.id.quick_actions
        }
    }

    fun createSmartbarView(): LinearLayout {
        val smartbarView = View.inflate(florisboard.context, R.layout.smartbar, null) as LinearLayout

        candidateViewList.add(smartbarView.findViewById(R.id.candidate0))
        candidateViewList.add(smartbarView.findViewById(R.id.candidate1))
        candidateViewList.add(smartbarView.findViewById(R.id.candidate2))

        candidatesView = smartbarView.findViewById(R.id.candidates)
        numberRowView = smartbarView.findViewById(R.id.number_row)
        quickActionsView = smartbarView.findViewById(R.id.quick_actions)

        quickActionToggle = smartbarView.findViewById(R.id.quick_action_toggle)
        quickActionToggle?.setOnClickListener(quickActionToggleOnClickListener)
        quickActionTogglePlaceholder = smartbarView.findViewById(R.id.quick_action_toggle_placeholder)

        val quickActions = smartbarView.findViewById<LinearLayout>(R.id.quick_actions)
        for (quickAction in quickActions.children) {
            if (quickAction is ImageButton) {
                quickAction.setOnClickListener(quickActionOnClickListener)
            }
        }
        val numberRow = smartbarView.findViewById<LinearLayout>(R.id.number_row)
        for (numberRowButton in numberRow.children) {
            if (numberRowButton is Button) {
                numberRowButton.setOnClickListener(numberRowButtonOnClickListener)
            }
        }
        for (candidateView in candidateViewList) {
            candidateView.setOnClickListener(candidateViewOnClickListener)
            candidateView.setOnLongClickListener(candidateViewOnLongClickListener)
        }

        this.smartbarView = smartbarView

        return smartbarView
    }

    override fun onGetSuggestions(arr: Array<out SuggestionsInfo>?) {
        if (arr == null || arr.isEmpty()) {
            return
        }
        /*val suggestions = arr[0]
        for (i in 0 until suggestions.suggestionsCount) {
            candidateViewList[i].text = suggestions.getSuggestionAt(i)
            if (i == 2) {
                break
            }
        }*/
    }

    override fun onGetSentenceSuggestions(arr: Array<out SentenceSuggestionsInfo>?) {
        if (arr == null || arr.isEmpty()) {
            return
        }
        /*val suggestions = arr[0].getSuggestionsInfoAt(0)
        for (i in 0 until suggestions.suggestionsCount) {
            candidateViewList[i].text = suggestions.getSuggestionAt(i)
            if (i == 2) {
                break
            }
        }*/
    }

    fun onStartInputView(keyboardMode: KeyboardMode, isComposingEnabled: Boolean) {
        this.isComposingEnabled = isComposingEnabled
        when {
            keyboardMode == KeyboardMode.NUMERIC ||
            keyboardMode == KeyboardMode.PHONE ||
            keyboardMode == KeyboardMode.PHONE2 -> {
                smartbarView?.visibility = View.GONE
            }
            !isComposingEnabled -> {
                smartbarView?.visibility = View.VISIBLE
                activeContainerId = R.id.number_row
            }
            else -> {
                smartbarView?.visibility = View.VISIBLE
                activeContainerId = R.id.candidates
                val tsm = florisboard.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
                spellCheckerSession = tsm.newSpellCheckerSession(null, null, this, true)
            }
        }
    }

    fun onFinishInputView() {
        spellCheckerSession?.close()
    }

    fun deleteCandidateFromDictionary(candidate: String) {
        //
    }

    fun resetCandidates() {
        //
    }

    fun generateCandidatesFromComposing(composingText: String?) {
        if (composingText == null) {
            candidateViewList[0].text = "candidate"
            candidateViewList[1].text = "suggestions"
            candidateViewList[2].text = "nyi"
        } else {
            activeContainerId = R.id.candidates
            updateActiveContainerVisibility()
            candidateViewList[0].text = ""
            candidateViewList[1].text = composingText + "test"
            candidateViewList[2].text = ""
        }
        //spellCheckerSession?.getSentenceSuggestions(arrayOf(TextInfo(composing)), 3)
        //android.util.Log.i("SPELL", "GEN")
        /*val dic: Uri = UserDictionary.Words.CONTENT_URI
        val resolver: ContentResolver = florisboard.contentResolver
        val cursor: Cursor = resolver.query(dic, null, null, null, null) ?: return
        var count = 0
        while (cursor.moveToNext()) {
            val word = cursor.getString(cursor.getColumnIndex(UserDictionary.Words.WORD))
            candidateViewList[count].text = word
            if (count++ > 2) {
                break
            }
        }
        cursor.close()*/
    }

    fun writeCandidate(candidate: String) {
        //
    }

    fun getPreferredContainerId(): Int {
        return when {
            !isComposingEnabled -> when(textInputManager.getActiveKeyboardMode()) {
                KeyboardMode.CHARACTERS -> R.id.number_row
                else -> 0
            }
            else -> R.id.candidates
        }
    }

    private fun updateActiveContainerVisibility() {
        when (activeContainerId) {
            R.id.quick_actions -> {
                candidatesView?.visibility = View.GONE
                numberRowView?.visibility = View.GONE
                quickActionsView?.visibility = View.VISIBLE
                quickActionToggle?.rotation = -180.0f
            }
            R.id.number_row -> {
                candidatesView?.visibility = View.GONE
                numberRowView?.visibility = View.VISIBLE
                quickActionsView?.visibility = View.GONE
                quickActionToggle?.rotation = 0.0f
            }
            R.id.candidates -> {
                candidatesView?.visibility = View.VISIBLE
                numberRowView?.visibility = View.GONE
                quickActionsView?.visibility = View.GONE
                quickActionToggle?.rotation = 0.0f
            }
            else -> {
                candidatesView?.visibility = View.GONE
                numberRowView?.visibility = View.GONE
                quickActionsView?.visibility = View.GONE
                quickActionToggle?.rotation = 0.0f
            }
        }
    }

    fun setSmartbarHeightFactor(factor: Float) {
        val baseSize = florisboard.context.resources.getDimension(R.dimen.smartbar_button_width)
        val size = (baseSize * factor).toInt()
        quickActionToggle?.layoutParams?.width = size
        quickActionToggle?.layoutParams?.height = size
        quickActionTogglePlaceholder?.layoutParams?.width = size
        quickActionTogglePlaceholder?.layoutParams?.height = size
    }
}
