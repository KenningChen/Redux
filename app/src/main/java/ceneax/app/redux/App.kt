package ceneax.app.redux

import android.app.Application
import ceneax.app.lib.redux.IReduxLoadingDialog
import ceneax.app.lib.redux.Redux
import ceneax.app.redux.dialog.LoadingDialog
import kotlin.reflect.KClass

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Redux.config(loadingDialog = object : IReduxLoadingDialog<LoadingDialog> {
            override val dialog: KClass<LoadingDialog> = LoadingDialog::class

            override val defaultContent: String = ""
            override fun LoadingDialog.dismissDialog() {
                dismissDialog()
            }


            override fun LoadingDialog.setLoadingCancelable(boolean: Boolean) {
                setDialogCancelable(boolean)
            }

            override fun LoadingDialog.setLoadingContent(content: String) {
                setContent(content)
            }

        })
    }
}