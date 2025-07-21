package com.g3.soundify_musicplayer.ui.upload;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;

/**
 * Fragment for uploading new songs or editing existing ones
 * Supports both create and update operations based on arguments
 */
public class UploadSongFragment extends Fragment {

    private UploadSongViewModel viewModel;

    // UI components
    private com.google.android.material.imageview.ShapeableImageView imageViewCoverArt;
    private TextView labelSelectCover;
    private Button buttonSelectAudio;
    private TextView textViewSelectedFile;
    private com.google.android.material.textfield.TextInputLayout textInputTitle;
    private com.google.android.material.textfield.TextInputEditText editTextTitle;
    private com.google.android.material.textfield.TextInputLayout textInputDescription;
    private com.google.android.material.textfield.TextInputEditText editTextDescription;
    private com.google.android.material.textfield.TextInputLayout textInputGenre;
    private AutoCompleteTextView autoCompleteGenre;
    private ImageView imageViewWaveform;
    private com.google.android.material.materialswitch.MaterialSwitch switchVisibility;
    private Button buttonDelete;
    private Button buttonSave;
    
    // Activity result launchers
    private ActivityResultLauncher<Intent> audioPickerLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    
    // Constants
    private static final String ARG_SONG_ID = "song_id";
    private static final String ARG_IS_EDIT_MODE = "is_edit_mode";
    
    /**
     * Create new instance for uploading a new song
     */
    public static UploadSongFragment newInstanceForUpload() {
        return new UploadSongFragment();
    }
    
    /**
     * Create new instance for editing an existing song
     */
    public static UploadSongFragment newInstanceForEdit(long songId) {
        UploadSongFragment fragment = new UploadSongFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SONG_ID, songId);
        args.putBoolean(ARG_IS_EDIT_MODE, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upload_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI components
        initializeViews(view);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(UploadSongViewModel.class);
        
        // Setup activity result launchers
        setupActivityResultLaunchers();
        
        // Setup UI
        setupUI();
        
        // Setup observers
        setupObservers();
        
        // Check if this is edit mode
        handleArguments();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews(View view) {
        imageViewCoverArt = view.findViewById(R.id.image_view_cover_art);
        labelSelectCover = view.findViewById(R.id.label_select_cover);
        buttonSelectAudio = view.findViewById(R.id.button_select_audio);
        textViewSelectedFile = view.findViewById(R.id.text_view_selected_file);
        textInputTitle = view.findViewById(R.id.text_input_title);
        editTextTitle = view.findViewById(R.id.edit_text_title);
        textInputDescription = view.findViewById(R.id.text_input_description);
        editTextDescription = view.findViewById(R.id.edit_text_description);
        textInputGenre = view.findViewById(R.id.text_input_genre);
        autoCompleteGenre = view.findViewById(R.id.auto_complete_genre);
        imageViewWaveform = view.findViewById(R.id.image_view_waveform);
        switchVisibility = view.findViewById(R.id.switch_visibility);
        buttonDelete = view.findViewById(R.id.button_delete);
        buttonSave = view.findViewById(R.id.button_save);
    }

    /**
     * Setup activity result launchers for file picking
     */
    private void setupActivityResultLaunchers() {
        // Audio file picker
        audioPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri audioUri = result.getData().getData();
                    if (audioUri != null) {
                        handleAudioSelection(audioUri);
                    }
                }
            }
        );

        // Image file picker
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        handleImageSelection(imageUri);
                    }
                }
            }
        );
    }

    /**
     * Setup UI components and listeners
     */
    private void setupUI() {
        // Setup genre dropdown
        setupGenreDropdown();
        
        // Setup click listeners
        setupClickListeners();
        
        // Setup toolbar if needed (for back navigation)
        setupToolbar();
    }

    /**
     * Setup genre dropdown with predefined options
     */
    private void setupGenreDropdown() {
        String[] genres = getResources().getStringArray(R.array.music_genres);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            genres
        );
        autoCompleteGenre.setAdapter(adapter);
    }

    /**
     * Setup click listeners for UI components
     */
    private void setupClickListeners() {
        // Cover art selection
        imageViewCoverArt.setOnClickListener(v -> selectCoverArt());
        labelSelectCover.setOnClickListener(v -> selectCoverArt());
        
        // Audio file selection
        buttonSelectAudio.setOnClickListener(v -> selectAudioFile());
        
        // Save button
        buttonSave.setOnClickListener(v -> saveSong());
        
        // Delete button
        buttonDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    /**
     * Setup toolbar for navigation
     */
    private void setupToolbar() {
        // Since we're in a fragment, we need to handle back navigation differently
        // This will be handled by the parent activity's fragment manager
    }

    /**
     * Setup observers for ViewModel LiveData
     */
    private void setupObservers() {
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showProgressIndicator();
            } else {
                hideProgressIndicator();
            }
        });

        // Observe current song (for edit mode)
        viewModel.getCurrentSong().observe(getViewLifecycleOwner(), this::populateFields);

        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showValidationErrors(error);
            }
        });

        // Observe success messages
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), success -> {
            if (success != null && !success.isEmpty()) {
                showSuccessMessage();
                navigateBack();
            }
        });
    }

    /**
     * Handle fragment arguments (edit mode vs create mode)
     */
    private void handleArguments() {
        Bundle args = getArguments();
        if (args != null && args.getBoolean(ARG_IS_EDIT_MODE, false)) {
            long songId = args.getLong(ARG_SONG_ID, -1);
            if (songId != -1) {
                viewModel.loadSongForEdit(songId);
                buttonDelete.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Select cover art image
     */
    private void selectCoverArt() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_cover_art)));
    }

    /**
     * Select audio file
     */
    private void selectAudioFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        audioPickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_audio_file)));
    }

    /**
     * Handle audio file selection
     */
    private void handleAudioSelection(Uri audioUri) {
        String fileName = getFileName(audioUri);
        textViewSelectedFile.setText(fileName);
        viewModel.setSelectedAudioFile(audioUri, fileName);

        // Auto-fill title if empty
        if (editTextTitle.getText().toString().trim().isEmpty()) {
            String titleFromFileName = extractTitleFromFileName(fileName);
            editTextTitle.setText(titleFromFileName);
        }
    }

    /**
     * Handle image selection for cover art
     */
    private void handleImageSelection(Uri imageUri) {
        imageViewCoverArt.setImageURI(imageUri);
        labelSelectCover.setVisibility(View.GONE);
        viewModel.setSelectedCoverArt(imageUri);
    }

    /**
     * Get file name from URI
     */
    private String getFileName(Uri uri) {
        String fileName = "Unknown";
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("UploadSongFragment", "Error getting file name", e);
            }
        }
        return fileName;
    }

    /**
     * Extract title from file name
     */
    private String extractTitleFromFileName(String fileName) {
        if (fileName == null) return "";
        
        // Remove file extension
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileName = fileName.substring(0, lastDotIndex);
        }
        
        // Replace underscores and hyphens with spaces
        return fileName.replace("_", " ").replace("-", " ").trim();
    }

    /**
     * Save the song
     */
    private void saveSong() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String genre = autoCompleteGenre.getText().toString().trim();
        boolean isPublic = switchVisibility.isChecked();

        viewModel.saveSong(title, description, genre, isPublic);
    }

    /**
     * Show delete confirmation dialog
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_song)
            .setMessage(R.string.delete_song_confirmation)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                viewModel.deleteSong();
                navigateBack();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    /**
     * Navigate back to previous screen
     */
    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            requireActivity().finish();
        }
    }

    // UI Helper Methods
    private void showProgressIndicator() {
        buttonSave.setEnabled(false);
        buttonSave.setText(R.string.saving);
    }

    private void hideProgressIndicator() {
        buttonSave.setEnabled(true);
        buttonSave.setText(R.string.save);
    }

    private void showSuccessMessage() {
        Toast.makeText(requireContext(), R.string.song_saved_successfully, Toast.LENGTH_SHORT).show();
    }

    private void showErrorMessage() {
        Toast.makeText(requireContext(), R.string.error_saving_song, Toast.LENGTH_SHORT).show();
    }

    private void showValidationErrors(String error) {
        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
    }

    private void populateFields(Song song) {
        if (song != null) {
            editTextTitle.setText(song.getTitle());
            editTextDescription.setText(song.getDescription());
            autoCompleteGenre.setText(song.getGenre());
            switchVisibility.setChecked(song.isPublic());
            
            // Load cover art if available
            if (song.getCoverArtUrl() != null && !song.getCoverArtUrl().isEmpty()) {
                // Load cover art using Glide or similar
                // Glide.with(this).load(song.getCoverArtUrl()).into(imageViewCoverArt);
            }
        }
    }
}
