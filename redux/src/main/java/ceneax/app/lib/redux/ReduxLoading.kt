package ceneax.app.lib.redux

import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import ceneax.app.lib.redux.simple.RContext
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

interface IReduxLoadingDialog<D : DialogFragment> {
    val dialog: KClass<D>

    val defaultContent: String


    fun D.setLoadingContent(content: String)
    fun D.setLoadingCancelable(boolean: Boolean)
    fun D.dismissDialog()
}

internal class ReduxLoadingDialog : IReduxLoadingDialog<DialogFragment> {
    override val dialog: KClass<DialogFragment> = DialogFragment::class

    override val defaultContent: String = ""

    override fun DialogFragment.setLoadingCancelable(boolean: Boolean) {}

    override fun DialogFragment.setLoadingContent(content: String) {}

    override fun DialogFragment.dismissDialog() {}
}

class ReduxLoadingDialogContext(
    internal val dialogInstance: DialogFragment,
    internal val reduxLoadingDialog: IReduxLoadingDialog<DialogFragment>
) {
    fun setLoadingContent(content: String) = with(reduxLoadingDialog) {
        dialogInstance.setLoadingContent(content)
    }

    fun setCancelable(cancel: Boolean) = with(reduxLoadingDialog) {
        dialogInstance.setLoadingCancelable(cancel)
    }

    fun dismissDialog() = dialogInstance.dismissAllowingStateLoss()
}

suspend fun <T> EffectContext.loadingScope(
    autoDismiss: Boolean = true,
    block: suspend CoroutineScope.(ReduxLoadingDialogContext) -> T
): T = loadingDialogContext.let {
    if (fragmentManager.findFragmentByTag(this::class.java.simpleName) != null) {
        return@let lifecycleOwner.lifecycleScope.block(it)
    }

    fragmentManager.beginTransaction().remove(it.dialogInstance).commit()
    it.dialogInstance.show(fragmentManager, this::class.java.simpleName)
    it.setLoadingContent(it.reduxLoadingDialog.defaultContent)
    val result = lifecycleOwner.lifecycleScope.block(it)
    if (autoDismiss) {
        it.dialogInstance.dismissAllowingStateLoss()
    }
    result
}

suspend fun <T> RContext.loadingScope(
    autoDismiss: Boolean = true,
    block: suspend CoroutineScope.(ReduxLoadingDialogContext) -> T
): T = loadingDialogContext.let {
    if (fragmentManager.findFragmentByTag(this::class.java.simpleName) != null) {
        return@let coroutineScope.block(it)
    }

    fragmentManager.beginTransaction().remove(it.dialogInstance).commit()
    it.dialogInstance.show(fragmentManager, this::class.java.simpleName)
    it.setLoadingContent(it.reduxLoadingDialog.defaultContent)
    val result = coroutineScope.block(it)
    if (autoDismiss) {
        it.dialogInstance.dismissAllowingStateLoss()
    }
    result
}