package com.titanbiosync.gym.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.titanbiosync.data.local.entities.gym.ExerciseMediaEntity
import com.titanbiosync.databinding.DialogExerciseVideoBinding
import com.titanbiosync.gym.media.ExerciseMediaUriResolver
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class ExerciseVideoDialogFragment : DialogFragment() {

    private var _binding: DialogExerciseVideoBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var videos: List<ExerciseMediaEntity>
    private var selected: ExerciseMediaEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogExerciseVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }

        videos = readVideosArgs()
            .filter { it.type == "video" }
            .sortedWith(
                compareBy<ExerciseMediaEntity> {
                    when (it.source) {
                        "remote" -> 0
                        "asset" -> 1
                        else -> 2
                    }
                }.thenBy { it.id }
            )

        val online = videos.firstOrNull { it.source == "remote" }
        val asset = videos.firstOrNull { it.source == "asset" }

        val hasOnline = online != null
        val hasAsset = asset != null

        binding.sourceChooser.isVisible = hasOnline && hasAsset
        binding.btnOnline.isEnabled = hasOnline
        binding.btnAsset.isEnabled = hasAsset

        binding.btnOnline.setOnClickListener { online?.let(::selectAndPlay) }
        binding.btnAsset.setOnClickListener { asset?.let(::selectAndPlay) }

        // Default: remote > asset > primo disponibile
        (online ?: asset ?: videos.firstOrNull())?.let(::selectAndPlay)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    private fun selectAndPlay(media: ExerciseMediaEntity) {
        selected = media
        updateToggleUi()

        val uri = ExerciseMediaUriResolver.resolveVideoUri(source = media.source, url = media.url)

        val p = player ?: ExoPlayer.Builder(requireContext()).build().also { newPlayer ->
            player = newPlayer
            binding.playerView.player = newPlayer
            newPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }

        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()
        p.playWhenReady = true
    }

    private fun updateToggleUi() {
        val src = selected?.source
        // “selezionato” = disabilitato (semplice e chiaro)
        binding.btnOnline.isEnabled = (videos.any { it.source == "remote" }) && src != "remote"
        binding.btnAsset.isEnabled = (videos.any { it.source == "asset" }) && src != "asset"
    }

    private fun readVideosArgs(): List<ExerciseMediaEntity> {
        val encoded = requireArguments().getString(ARG_VIDEOS_JSON).orEmpty()
        return json.decodeFromString(encoded)
    }

    override fun onStop() {
        binding.playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_VIDEOS_JSON = "videos_json"

        fun newInstance(videos: List<ExerciseMediaEntity>): ExerciseVideoDialogFragment {
            val json = Json { ignoreUnknownKeys = true }
            return ExerciseVideoDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_VIDEOS_JSON, json.encodeToString(videos))
                }
            }
        }
    }
}