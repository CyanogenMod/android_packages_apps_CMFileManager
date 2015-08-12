package com.cyanogenmod.filemanager.tasks;

import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.FolderUsage;

/**
 * Created by bird on 8/13/15.
 */
public class FolderSizeAsyncListener implements AsyncResultListener {

    FileSystemObject mFso;

    public FolderSizeAsyncListener(FileSystemObject fso) {
        this.mFso = fso;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAsyncStart() {
        // this.mDrawingFolderUsage = false;
        // this.mFolderUsage = new FolderUsage(this.mFso.getFullPath());
        // printFolderUsage(true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAsyncEnd(final boolean cancelled) {
        // try {
        //     // Clone the reference
        //     FsoPropertiesDialog.this.mFolderUsage =
        //             (FolderUsage)this.mFolderUsageExecutable.getFolderUsage().clone();
        //     printFolderUsage(true, cancelled);
        //} catch (Exception ex) {/**NON BLOCK**/}
    }

    @Override
    public void onAsyncExitCode(int exitCode) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPartialResult(final Object partialResults) {
        long thing = ((FolderUsage)partialResults).getTotalSize();
        //    printFolderUsage(true, false);
        //} catch (Exception ex) {/**NON BLOCK**/}
    }

    @Override
    public void onException(Exception cause) {

    }

}
