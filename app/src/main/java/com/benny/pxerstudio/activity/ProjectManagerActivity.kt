package com.benny.pxerstudio.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.benny.pxerstudio.R
import com.benny.pxerstudio.databinding.ActivityProjectManagerBinding
import com.benny.pxerstudio.databinding.ItemProjectBinding
import com.benny.pxerstudio.exportable.ExportingUtils
import com.benny.pxerstudio.util.displayToast
import com.benny.pxerstudio.util.prompt
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.select.getSelectExtension
import java.io.File
import java.io.FileFilter

class ProjectManagerActivity : AppCompatActivity() {

    private var projects = ArrayList<File>()
    private lateinit var fa: FastAdapter<Item>
    private lateinit var ia: ItemAdapter<Item>
    private lateinit var binding: ActivityProjectManagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.projectManagerToolbar)

        binding.projectManagerCM.cMRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        ia = ItemAdapter()
        fa = FastAdapter.with(ia)

        fa.getSelectExtension().isSelectable = false
        binding.projectManagerCM.cMRecyclerView.adapter = fa

        projects.clear()

        //Find all projects
        val parent = File(ExportingUtils.getProjectPath(this))
        if (parent.exists()) {
            val temp = parent.listFiles(PxerFileFilter())
            for (i in temp!!.indices) {
                projects.add(temp[i])
            }
            if (projects.size >= 1) {
                binding.projectManagerCM.cMNoProjectFound.isGone = true

                for (i in projects.indices) {
                    val mName = projects[i].name.substring(0, projects[i].name.lastIndexOf('.'))
                    val mPath = projects[i].path
                    val imgPath = parent.path + "/" + mName + ".png"
                    ia.add(Item(mName, mPath, imgPath))
                }

                fa.onClickListener = { _, _, item, _ ->
                    val newIntent = Intent()
                    newIntent.putExtra("selectedProjectPath", item.path)

                    setResult(RESULT_OK, newIntent)
                    finish()
                    true
                }

                fa.onLongClickListener = { v, _, _, position ->
                    val pm = PopupMenu(v.context, v)
                    pm.inflate(R.menu.menu_popup_project)
                    pm.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.menu_popup_project_rename ->
                                MaterialDialog(this).show {
                                    title = getString(R.string.rename)
                                    input(hint = projects[position].name) { _, text ->
                                        var mInput = "$text"
                                        if (!mInput.endsWith(".pxer"))
                                            mInput += ".pxer"

                                        val fromFile = File(projects[position].path)
                                        val newFile = File(projects[position].parent, mInput)

                                        val oldPrev = File(fromFile.path.substring(0, fromFile.path.lastIndexOf('.')) + ".png")
                                        val newPrev = File(newFile.parent, mInput.substring(0, mInput.lastIndexOf('.')) + ".png")

                                        if (fromFile.renameTo(newFile)) {
                                            oldPrev.renameTo(newPrev);
                                            projects[position] = newFile
                                            ia[position] = Item(newFile.name, newFile.path, parent.path + newFile.name + ".png")
                                            fa.notifyAdapterItemChanged(position)

                                            val newIntent = Intent()
                                            newIntent.putExtra("fileNameChanged", true)

                                            setResult(RESULT_OK, newIntent)
                                        }
                                    }
                                    positiveButton(android.R.string.ok)
                                }

                            R.id.menu_popup_project_delete -> prompt()
                                .title(R.string.delete_project)
                                .message(R.string.delete_project_warning)
                                .positiveButton(R.string.delete).positiveButton {
                                    var filePath = projects[position].absolutePath
                                    if (projects[position].delete()) {
                                        File(filePath.substring(0, filePath.lastIndexOf('.')) + ".png").delete() // remove project preview
                                        ia.remove(position)
                                        projects.removeAt(position)

                                        if (projects.size < 1)
                                            binding.projectManagerCM.cMNoProjectFound.isVisible =
                                                true

                                        val newIntent = Intent()
                                        newIntent.putExtra("fileNameChanged", true)

                                        setResult(RESULT_OK, newIntent)

                                        displayToast(R.string.project_deleted)
                                    } else
                                        displayToast(R.string.error_deleting_project)
                                }.show()
                        }
                        true
                    }
                    pm.show()
                    true
                }
            }
        }
    }

    inner class PxerFileFilter : FileFilter {
        override fun accept(pathname: File): Boolean {
            return pathname.name.endsWith(".pxer")
        }
    }

    class Item(var title: String, var path: String, var img_path: String?) : AbstractItem<Item.ViewHolder>() {

        override val type: Int
            get() = 0

        override val layoutRes: Int
            get() = R.layout.item_project

        override fun getViewHolder(v: View): ViewHolder {
            return ViewHolder(v)
        }

        class ViewHolder internal constructor(view: View) : FastAdapter.ViewHolder<Item>(view) {
            private var itemProjectBinding = ItemProjectBinding.bind(view)

            override fun bindView(item: Item, payloads: List<Any>) {
                itemProjectBinding.itemProjectTitle.text = item.title
                itemProjectBinding.itemProjectPath.text = item.path
                if (File(item.img_path).exists())
                    itemProjectBinding.itemProjectImageView.setImageURI(Uri.parse(item.img_path))
            }

            override fun unbindView(item: Item) {
            }
        }
    }
}
