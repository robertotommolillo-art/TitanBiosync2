package com.titanbiosync.gym.ui.library

import android.os.Bundle
import android.os.Bundle.EMPTY
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.titanbiosync.R
import com.titanbiosync.data.local.entities.gym.GymFolderEntity
import com.titanbiosync.data.local.entities.gym.WorkoutTemplateEntity
import com.titanbiosync.databinding.FragmentGymLibraryBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GymLibraryFragment : Fragment() {

    private var _binding: FragmentGymLibraryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GymLibraryViewModel by viewModels()

    private lateinit var touchHelper: ItemTouchHelper

    private val adapter by lazy {
        LibraryAdapter(
            onUp = { viewModel.navigateUpToRoot() },
            onFolderClick = { folder -> viewModel.openFolder(folder) },
            onFolderEdit = { folder -> showRenameFolderDialog(folder) },
            onFolderDelete = { folder -> showDeleteFolderDialog(folder) },
            onTemplateClick = { template -> openTemplate(template) },
            onTemplateLongClick = { template -> showTemplateActions(template) },
            onStartDrag = { vh -> touchHelper.startDrag(vh) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGymLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.libraryRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.libraryRecycler.adapter = adapter

        binding.openCatalogButton.setOnClickListener {
            findNavController().navigate(R.id.gymExercisesFragment, EMPTY)
        }

        binding.addFab.setOnClickListener {
            showCreateDialog()
        }

        binding.upButton.setOnClickListener {
            viewModel.navigateUpToRoot()
        }

        viewModel.title.observe(viewLifecycleOwner) { title ->
            binding.title.text = title
        }

        viewModel.showUpButton.observe(viewLifecycleOwner) { show ->
            binding.upButton.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.libraryItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }

        touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val item = adapter.currentList.getOrNull(viewHolder.bindingAdapterPosition)
                return if (item is LibraryListItem.Template) {
                    makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                } else {
                    makeMovementFlags(0, 0)
                }
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition

                // vieta di spostare sopra cartelle o sopra ".."
                val targetItem = adapter.currentList.getOrNull(to)
                if (targetItem !is LibraryListItem.Template) return false

                viewModel.onTemplatesReordered(from, to)
                // la lista UI si aggiorna via LiveData; forziamo comunque un refresh soft
                adapter.submitList(viewModel.libraryItems.value.orEmpty().toList())
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // no-op
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewModel.commitTemplateOrder()
            }

            override fun isLongPressDragEnabled(): Boolean = false
        })
        touchHelper.attachToRecyclerView(binding.libraryRecycler)
    }

    private fun openTemplate(template: WorkoutTemplateEntity) {
        val args = Bundle().apply { putString("templateId", template.id) }
        findNavController().navigate(R.id.workoutTemplateDetailFragment, args)
    }

    // --- Dialog cartelle ---
    private fun showRenameFolderDialog(folder: GymFolderEntity) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Nome cartella"
            setText(folder.name)
            setSelection(folder.name.length)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Rinomina cartella")
            .setView(input)
            .setPositiveButton("Salva") { _, _ ->
                viewModel.renameFolder(folder.id, input.text?.toString().orEmpty())
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun showDeleteFolderDialog(folder: GymFolderEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminare cartella?")
            .setMessage("Le schede in questa cartella verranno spostate in \"Senza cartella\".")
            .setPositiveButton("Elimina") { _, _ ->
                viewModel.deleteFolder(folder.id)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // --- Dialog schede ---
    private fun showTemplateActions(template: WorkoutTemplateEntity) {
        val options = arrayOf("Rinomina", "Sposta", "Elimina")
        AlertDialog.Builder(requireContext())
            .setTitle(template.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameTemplateDialog(template)
                    1 -> showMoveTemplateDialog(template)
                    2 -> showDeleteTemplateDialog(template)
                }
            }
            .show()
    }

    private fun showRenameTemplateDialog(template: WorkoutTemplateEntity) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Nome scheda"
            setText(template.name)
            setSelection(template.name.length)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Rinomina scheda")
            .setView(input)
            .setPositiveButton("Salva") { _, _ ->
                viewModel.renameTemplate(template.id, input.text?.toString().orEmpty())
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun showDeleteTemplateDialog(template: WorkoutTemplateEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminare scheda?")
            .setMessage("Verrà eliminata la scheda e tutti i suoi esercizi.")
            .setPositiveButton("Elimina") { _, _ ->
                viewModel.deleteTemplate(template.id)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun showMoveTemplateDialog(template: WorkoutTemplateEntity) {
        val folders = viewModel.folders.value.orEmpty()

        val labels = ArrayList<String>(folders.size + 1).apply {
            add("Senza cartella")
            addAll(folders.map { it.name })
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Sposta scheda")
            .setItems(labels.toTypedArray()) { _, index ->
                val newFolderId = if (index == 0) null else folders[index - 1].id
                viewModel.moveTemplate(template.id, newFolderId)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // --- Create ---
    private fun showCreateDialog() {
        val inFolder = viewModel.showUpButton.value == true
        val options = if (inFolder) {
            arrayOf("Nuova scheda")
        } else {
            arrayOf("Nuova cartella", "Nuova scheda")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Aggiungi")
            .setItems(options) { _, which ->
                if (inFolder) {
                    showCreateTemplateDialog()
                } else {
                    when (which) {
                        0 -> showCreateFolderDialog()
                        1 -> showCreateTemplateDialog()
                    }
                }
            }
            .show()
    }

    private fun showCreateFolderDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Nome cartella"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Nuova cartella")
            .setView(input)
            .setPositiveButton("Crea") { _, _ ->
                viewModel.createFolder(input.text?.toString().orEmpty())
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun showCreateTemplateDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Nome scheda"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Nuova scheda")
            .setView(input)
            .setPositiveButton("Crea") { _, _ ->
                viewModel.createTemplate(input.text?.toString().orEmpty())
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}