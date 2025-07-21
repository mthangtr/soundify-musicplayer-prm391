package com.g3.soundify_musicplayer.utils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.ui.upload.UploadSongFragment;

/**
 * Helper class for fragment navigation
 */
public class NavigationHelper {
    
    /**
     * Navigate to UploadSongFragment for creating a new song
     */
    public static void navigateToUploadSong(FragmentManager fragmentManager) {
        UploadSongFragment uploadFragment = UploadSongFragment.newInstanceForUpload();
        
        fragmentManager.beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,  // enter
                        R.anim.slide_out_left,  // exit
                        R.anim.slide_in_left,   // popEnter
                        R.anim.slide_out_right  // popExit
                )
                .replace(R.id.fragment_container, uploadFragment)
                .addToBackStack("upload_song")
                .commit();
    }
    
    /**
     * Navigate to UploadSongFragment for editing an existing song
     */
    public static void navigateToEditSong(FragmentManager fragmentManager, long songId) {
        UploadSongFragment uploadFragment = UploadSongFragment.newInstanceForEdit(songId);
        
        fragmentManager.beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,  // enter
                        R.anim.slide_out_left,  // exit
                        R.anim.slide_in_left,   // popEnter
                        R.anim.slide_out_right  // popExit
                )
                .replace(R.id.fragment_container, uploadFragment)
                .addToBackStack("edit_song")
                .commit();
    }
    
    /**
     * Check if current fragment is UploadSongFragment
     */
    public static boolean isUploadFragmentVisible(FragmentManager fragmentManager) {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        return currentFragment instanceof UploadSongFragment;
    }
    
    /**
     * Pop back stack if there are fragments
     */
    public static boolean handleBackPress(FragmentManager fragmentManager) {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
            return true;
        }
        return false;
    }
}
