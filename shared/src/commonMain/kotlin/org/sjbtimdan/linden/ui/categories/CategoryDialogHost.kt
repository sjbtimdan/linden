package org.sjbtimdan.linden.ui.categories

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.categories_duplicate_name
import org.sjbtimdan.linden.resources.common_name_required

/**
 * Hosts [CategoryDialog] for [state]: validates the name, reports a duplicate
 * through the state's name error, and closes the dialog on a successful save.
 * Shared by the category list and the entry form's "+ New" chip.
 */
@Composable
fun CategoryDialogHost(
    state: CategoryDialogState?,
    isEditing: Boolean,
    canDelete: Boolean,
    onStateChange: (CategoryDialogState?) -> Unit,
    onSave: (name: String, type: CategoryType, icon: CategoryIcon?) -> Boolean,
    onDelete: (Category) -> Unit = {},
) {
    val dialogState = state ?: return
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val nameRequiredError = stringResource(Res.string.common_name_required)
    val duplicateNameError = stringResource(Res.string.categories_duplicate_name)

    CategoryDialog(
        name = dialogState.name,
        type = dialogState.type,
        icon = dialogState.icon,
        nameError = dialogState.nameError,
        isEditing = isEditing,
        canDelete = canDelete,
        onNameChange = { onStateChange(dialogState.copy(name = it, nameError = null)) },
        onTypeChange = { onStateChange(dialogState.copy(type = it)) },
        onIconChange = { onStateChange(dialogState.copy(icon = it)) },
        onDelete = {
            dialogState.category?.let { existing ->
                onStateChange(null)
                onDelete(existing)
            }
        },
        onSave = {
            val name = dialogState.name.trim()
            if (name.isEmpty()) {
                onStateChange(dialogState.copy(nameError = nameRequiredError))
            } else {
                val saved = onSave(name, dialogState.type, dialogState.icon)
                if (saved) {
                    onStateChange(null)
                    keyboardController?.hide()
                    focusManager.clearFocus()
                } else {
                    onStateChange(dialogState.copy(nameError = duplicateNameError))
                }
            }
        },
        onDismiss = { onStateChange(null) },
    )
}
