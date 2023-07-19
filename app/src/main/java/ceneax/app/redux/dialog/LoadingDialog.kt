package ceneax.app.redux.dialog

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import ceneax.app.redux.R

class LoadingDialog : DialogFragment() {
    private var mTvContent: TextView? = null

    private var cancelable = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_loading, null, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mTvContent = view.findViewById(R.id.tvContent)
    }

    fun setContent(content: String) {
        mTvContent?.text = content
    }

    fun setDialogCancelable(cancelable: Boolean) {
        this.cancelable = cancelable
    }

    fun dismissDialog(){
        dismiss()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager,tag)
        runPost {
            dialog?.apply {
                setCancelable(cancelable)
                setCanceledOnTouchOutside(cancelable)
            }
        }
    }

}

val globalMainHandler = Handler(Looper.getMainLooper())

inline fun runPost(crossinline block: () -> Unit) {
    runPost(block, null)
}

inline fun runPost(crossinline block: () -> Unit, delayMillis: Long? = null) {
    globalMainHandler.apply {
        if (delayMillis == null) {
            post {
                block()
            }
        } else {
            postDelayed({
                block()
            }, delayMillis)
        }
    }
}