package com.g3.soundify_musicplayer.ui.upload;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;

/**
 * Activity for uploading new songs or editing existing ones
 * Supports both create and update operations based on intent extras
 */
public class UploadSongActivity extends AppCompatActivity {

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
    private static final String EXTRA_SONG_ID = "song_id";
    private static final String EXTRA_IS_EDIT_MODE = "is_edit_mode";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_song);

        // Initialize UI components
        initializeViews();
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(UploadSongViewModel.class);
        
        // Setup activity result launchers
        setupActivityResultLaunchers();
        
        // Setup UI
        setupUI();
        
        // Setup observers
        setupObservers();
        
        // Check if this is edit mode
        handleIntent();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        imageViewCoverArt = findViewById(R.id.image_view_cover_art);
        labelSelectCover = findViewById(R.id.label_select_cover);
        buttonSelectAudio = findViewById(R.id.button_select_audio);
        textViewSelectedFile = findViewById(R.id.text_view_selected_file);
        textInputTitle = findViewById(R.id.text_input_title);
        editTextTitle = findViewById(R.id.edit_text_title);
        textInputDescription = findViewById(R.id.text_input_description);
        editTextDescription = findViewById(R.id.edit_text_description);
        textInputGenre = findViewById(R.id.text_input_genre);
        autoCompleteGenre = findViewById(R.id.auto_complete_genre);
        imageViewWaveform = findViewById(R.id.image_view_waveform);
        switchVisibility = findViewById(R.id.switch_visibility);
        buttonDelete = findViewById(R.id.button_delete);
        buttonSave = findViewById(R.id.button_save);
    }
    
    /**
     * Setup activity result launchers for file selection
     */
    private void setupActivityResultLaunchers() {
        // Audio file picker
        audioPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri audioUri = result.getData().getData();
                    if (audioUri != null) {
                        String fileName = getFileName(audioUri);
                        viewModel.setSelectedAudioFile(audioUri, fileName);
                    }
                }
            }
        );
        
        // Image file picker for cover art
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        viewModel.setSelectedCoverArt(imageUri);
                        imageViewCoverArt.setImageURI(imageUri);
                    }
                }
            }
        );
    }
    
    /**
     * Setup UI components
     */
    private void setupUI() {
        // Setup genre dropdown
        setupGenreDropdown();
        
        // Setup click listeners
        setupClickListeners();
        
        // Setup toolbar
        setupToolbar();
    }
    
    /**
     * Setup genre dropdown with predefined options
     */
    private void setupGenreDropdown() {
        String[] genres = getResources().getStringArray(R.array.genre_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            genres
        );
        autoCompleteGenre.setAdapter(adapter);
    }
    
    /**
     * Setup click listeners for buttons and interactive elements
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
     * Setup toolbar with back navigation
     */
    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    /**
     * Setup LiveData observers
     */
    private void setupObservers() {
        // Observe edit mode
        viewModel.getIsEditMode().observe(this, isEditMode -> {
            if (isEditMode != null) {
                updateUIForMode(isEditMode);
            }
        });
        
        // Observe current song data
        viewModel.getCurrentSong().observe(this, this::populateFields);
        
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                buttonSave.setEnabled(!isLoading);
                buttonDelete.setEnabled(!isLoading);
                // You can add a progress indicator here if needed
            }
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                viewModel.clearErrorMessage();
            }
        });
        
        // Observe success messages
        viewModel.getSuccessMessage().observe(this, successMessage -> {
            if (successMessage != null && !successMessage.isEmpty()) {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
                viewModel.clearSuccessMessage();
                // Close activity on success
                finish();
            }
        });
        
        // Observe selected audio file
        viewModel.getAudioFileName().observe(this, fileName -> {
            if (fileName != null && !fileName.isEmpty()) {
                textViewSelectedFile.setText(fileName);
                textViewSelectedFile.setVisibility(View.VISIBLE);
            } else {
                textViewSelectedFile.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * Handle intent extras to determine mode and load data
     */
    private void handleIntent() {
        Intent intent = getIntent();
        boolean isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false);
        
        if (isEditMode) {
            long songId = intent.getLongExtra(EXTRA_SONG_ID, -1);
            if (songId != -1) {
                viewModel.loadSongForEdit(songId);
            } else {
                Toast.makeText(this, "Invalid song ID", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            viewModel.initializeForNewSong();
        }
    }
    
    /**
     * Update UI based on current mode (create/edit)
     */
    private void updateUIForMode(boolean isEditMode) {
        if (isEditMode) {
            setTitle(getString(R.string.edit_song_title));
            buttonDelete.setVisibility(View.VISIBLE);
        } else {
            setTitle(getString(R.string.upload_song_title));
            buttonDelete.setVisibility(View.GONE);
        }
    }
    
    /**
     * Populate form fields with song data
     */
    private void populateFields(Song song) {
        if (song != null) {
            editTextTitle.setText(song.getTitle());
            editTextDescription.setText(song.getDescription());
            autoCompleteGenre.setText(song.getGenre(), false);
            switchVisibility.setChecked(song.isPublic());

            // Load cover art if available
            if (song.getCoverArtUrl() != null && !song.getCoverArtUrl().isEmpty()) {
                try {
                    Uri coverUri = Uri.parse(song.getCoverArtUrl());
                    imageViewCoverArt.setImageURI(coverUri);
                } catch (Exception e) {
                    // Keep default image if URI is invalid
                }
            }
        }
    }
    
    /**
     * Open file picker for audio selection
     */
    private void selectAudioFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        // You can be more specific: intent.setType("audio/mpeg");
        audioPickerLauncher.launch(intent);
    }
    
    /**
     * Open file picker for cover art selection
     */
    private void selectCoverArt() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }
    
    /**
     * Save song with current form data
     */
    private void saveSong() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String genre = autoCompleteGenre.getText().toString().trim();
        boolean isPublic = switchVisibility.isChecked();

        viewModel.saveSong(title, description, genre, isPublic);
    }
    
    /**
     * Show confirmation dialog for song deletion
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_song_title)
            .setMessage(R.string.delete_song_message)
            .setPositiveButton(R.string.delete_confirm, (dialog, which) -> {
                viewModel.deleteSong();
            })
            .setNegativeButton(R.string.button_cancel, null)
            .show();
    }
    
    /**
     * Get file name from URI
     */
    private String getFileName(Uri uri) {
        String fileName = null;
        
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                // Ignore errors
            }
        }
        
        if (fileName == null) {
            fileName = uri.getPath();
            if (fileName != null) {
                int cut = fileName.lastIndexOf('/');
                if (cut != -1) {
                    fileName = fileName.substring(cut + 1);
                }
            }
        }
        
        return fileName != null ? fileName : "audio_file.mp3";
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Add smooth back transition animation
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up references
        imageViewCoverArt = null;
        labelSelectCover = null;
        buttonSelectAudio = null;
        textViewSelectedFile = null;
        textInputTitle = null;
        editTextTitle = null;
        textInputDescription = null;
        editTextDescription = null;
        textInputGenre = null;
        autoCompleteGenre = null;
        imageViewWaveform = null;
        switchVisibility = null;
        buttonDelete = null;
        buttonSave = null;
    }
    
    /**
     * Static method to create intent for new song upload
     */
    public static Intent createUploadIntent(Activity context) {
        return new Intent(context, UploadSongActivity.class);
    }
    
    /**
     * Static method to create intent for song editing
     */
    public static Intent createEditIntent(Activity context, long songId) {
        Intent intent = new Intent(context, UploadSongActivity.class);
        intent.putExtra(EXTRA_SONG_ID, songId);
        intent.putExtra(EXTRA_IS_EDIT_MODE, true);
        return intent;
    }
}
