package ceneax.app.lib.redux

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import java.io.Serializable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class ReduxDialog<VB : ViewBinding, P> : DialogFragment() {
    private val mSubscribe = Subscribe<Result<P>>()
    private var mResult = Result<P>()

    private var _viewBinding: VB? = null
    protected val vb get() = _viewBinding!!

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val cls: Class<VB> = this::class.getGenericsClass(0)
        _viewBinding = cls.getDeclaredMethod("inflate", LayoutInflater::class.java)
            .invoke(null, inflater) as VB
        return vb.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

//    @ReduxInternalApi
//    final override fun dismiss() {
//        super.dismiss()
//        mSubscribe.post(mResult)
//    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mSubscribe.post(mResult)
    }

//    @OptIn(ReduxInternalApi::class)
    open fun dismiss(type: Result<P>) {
        mResult = type
        dismissAllowingStateLoss()
    }

//    @ReduxInternalApi
//    final override fun show(manager: FragmentManager, tag: String?) {
//        super.show(manager, tag)
//    }

//    @OptIn(ReduxInternalApi::class)
    suspend fun showAsSuspend(manager: FragmentManager, tag: String? = null) = suspendCoroutine<Result<P>> {
        mSubscribe.onSubscribe { value ->
            it.resume(value)
        }
        show(manager, tag)
    }

    object Type {
        const val CANCEL = -1
        const val OK = 0
        const val NEUTRAL = 1
    }

    data class Result<P>(
        val type: Int = Type.CANCEL,
        val payload: P? = null
    ) : Serializable
}

inline fun <P> ReduxDialog.Result<P>.whenOk(block: (P?) -> Unit): ReduxDialog.Result<P> {
    if (type == ReduxDialog.Type.OK) {
        block(payload)
    }
    return this
}

inline fun <P> ReduxDialog.Result<P>.whenCancel(block: (P?) -> Unit): ReduxDialog.Result<P> {
    if (type == ReduxDialog.Type.CANCEL) {
        block(payload)
    }
    return this
}

inline fun <P> ReduxDialog.Result<P>.whenNeutral(block: (P?) -> Unit): ReduxDialog.Result<P> {
    if (type == ReduxDialog.Type.NEUTRAL) {
        block(payload)
    }
    return this
}

internal class Subscribe<T> {
    private var mCallback: (T) -> Unit = {}

    fun post(value: T) {
        mCallback(value)
    }

    fun onSubscribe(block: (T) -> Unit) {
        mCallback = block
    }
}