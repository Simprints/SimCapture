package org.dhis2.form.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.dhis2.form.model.FieldUiModelImpl
import org.dhis2.form.model.FormSection
import org.dhis2.form.simprints.ramp.ui.FIXED_SECTION_NEXT_BUTTON_TEST_TAG
import org.dhis2.form.ui.intent.FormIntent
import org.dhis2.form.ui.provider.inputfield.AgeProviderTest.Companion.FIELD_UI_MODEL_UID
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.mobile.ui.designsystem.component.SectionState
import org.hisp.dhis.mobile.ui.designsystem.resource.provideStringResource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FormTest {


    companion object {

        const val FORM_VIEW = "FORM_VIEW"
    }


    @get:Rule
    val composeTestRule = createComposeRule()


    @Test
    fun shouldDisplayEnrollmentFormCorrectly() {
        val sections = mutableListOf<FormSection>()
        val fields = listOf(FieldUiModelImpl(
            uid = FIELD_UI_MODEL_UID,
            value = "value",
            focused = false,
            error = null,
            editable = true,
            warning = null,
            mandatory = false,
            label = "label",
            programStageSection = null,
            hint = null,
            description = null,
            valueType = ValueType.AGE,
            legend = null,
            optionSet = null,
            allowFutureDates = null,
            uiEventFactory = null,
            displayName = null,
            renderingType = null,
            keyboardActionType = null,
            fieldMask = null,
            optionSetConfiguration = null,
            autocompleteList = null,
            orgUnitSelectorScope = null,
            selectableDates = null,

        ))
        sections.add(
            FormSection(
                uid = "uid",
                title = "item.label",
                description = "item.description",
                state = SectionState.OPEN,
                fields = fields,
                warningMessage = null,
                completeFields = 0,
                totalFields = 2,
                warnings = 0,
                errors = 0,
            ),
        )

       composeTestRule.setContent {
           Form(
               sections = sections,
               intentHandler = {},
               uiEventHandler = {},
           )
        }
        composeTestRule.onNodeWithTag(FORM_VIEW).assertIsDisplayed()

    }

    @Test
    fun shouldDisplayNextOnFixedSectionsExceptTheLast() {
        var nextLabel = ""
        composeTestRule.setContent {
            nextLabel = provideStringResource("action_next")
            Form(
                sections =
                    listOf(
                        section("fixed", SectionState.FIXED),
                        section("open", SectionState.OPEN),
                        section("last", SectionState.FIXED),
                    ),
                intentHandler = {},
                uiEventHandler = {},
            )
        }

        composeTestRule.onAllNodesWithTag(FIXED_SECTION_NEXT_BUTTON_TEST_TAG).assertCountEquals(1)
        composeTestRule.onAllNodesWithText(nextLabel).assertCountEquals(2)
    }

    @Test
    fun shouldOpenClosedNextSectionFromFixedSection() {
        val sectionIntents = mutableListOf<FormIntent.OnSection>()
        composeTestRule.setContent {
            Form(
                sections = listOf(section("fixed", SectionState.FIXED), section("closed", SectionState.CLOSE)),
                intentHandler = { (it as? FormIntent.OnSection)?.let(sectionIntents::add) },
                uiEventHandler = {},
            )
        }

        composeTestRule.onNodeWithTag(FIXED_SECTION_NEXT_BUTTON_TEST_TAG).performClick()

        assertEquals(listOf(FormIntent.OnSection("closed")), sectionIntents)
    }

    @Test
    fun shouldKeepAlreadyOpenNextSectionFromFixedSection() {
        val sectionIntents = mutableListOf<FormIntent.OnSection>()
        composeTestRule.setContent {
            Form(
                sections = listOf(section("fixed", SectionState.FIXED), section("open", SectionState.OPEN)),
                intentHandler = { (it as? FormIntent.OnSection)?.let(sectionIntents::add) },
                uiEventHandler = {},
            )
        }

        composeTestRule.onNodeWithTag(FIXED_SECTION_NEXT_BUTTON_TEST_TAG).performClick()

        assertEquals(emptyList<FormIntent.OnSection>(), sectionIntents)
    }

    private fun section(
        uid: String,
        state: SectionState,
    ) = FormSection(
        uid = uid,
        title = "Section $uid",
        description = null,
        state = state,
        fields =
            listOf(
                FieldUiModelImpl(
                    uid = "$uid-field",
                    value = null,
                    focused = false,
                    error = null,
                    editable = true,
                    warning = null,
                    mandatory = false,
                    label = "label",
                    programStageSection = uid,
                    hint = null,
                    description = null,
                    valueType = ValueType.TEXT,
                    legend = null,
                    optionSet = null,
                    allowFutureDates = null,
                    uiEventFactory = null,
                    displayName = null,
                    renderingType = null,
                    keyboardActionType = null,
                    fieldMask = null,
                    optionSetConfiguration = null,
                    autocompleteList = null,
                    orgUnitSelectorScope = null,
                    selectableDates = null,
                ),
            ),
        totalFields = 1,
    )
}