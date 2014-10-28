/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.filemanager.ui.policy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument.Page;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.PrintAttributes.Margins;
import android.print.PrintAttributes.MediaSize;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.ReadExecutable;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;
import com.cyanogenmod.filemanager.util.StringHelper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A class with the convenience methods to print documents
 */
public final class PrintActionPolicy extends ActionsPolicy {

    private static final String TAG = "PrintActionPolicy"; //$NON-NLS-1$

    private static final String PDF_FILE_EXT = "pdf";

    /**
     * Method that returns if the {@code FileSystemObject} can be printed
     *
     * @param ctx The current context
     * @param fso The fso to check
     * @return boolean If the fso can be printed
     */
    public static boolean isPrintedAllowed(Context ctx, FileSystemObject fso) {
        MimeTypeCategory category = MimeTypeHelper.getCategory(ctx, fso);
        String extension = FileHelper.getExtension(fso);
        return category.compareTo(MimeTypeCategory.TEXT) == 0
                || category.compareTo(MimeTypeCategory.IMAGE) == 0
                || (extension != null && extension.toLowerCase().equals(PDF_FILE_EXT));
    }

    /**
     * Method that prints the passed document
     *
     * @param ctx The current context
     * @param fso The document to print
     */
    public static void printDocument(final Context ctx, FileSystemObject fso) {
        MimeTypeCategory category = MimeTypeHelper.getCategory(ctx, fso);
        if (category.equals(MimeTypeCategory.TEXT)) {
            printTextDocument(ctx, fso);
            return;
        }
        if (category.equals(MimeTypeCategory.IMAGE)) {
            printImage(ctx, fso);
            return;
        }
        String ext = FileHelper.getExtension(fso);
        if (ext != null && ext.toLowerCase().equals(PDF_FILE_EXT)) {
            printPdfDocument(ctx, fso);
            return;
        }
        DialogHelper.showToast(ctx, R.string.print_unsupported_document, Toast.LENGTH_SHORT);
    }

    public static abstract class DocumentAdapterReader {
        /**
         * Read the document to an string array
         *
         * @param lines The array where to put the document
         * @param adjustedLines The array where to put the document
         */
        public abstract void read(List<String> lines, List<String> adjustedLines);

        /**
         * Read the document mode [0-Invalid; 1-Text; 2-Binary]
         *
         * @return int The document mode
         */
        public abstract int getDocumentMode();
    }

    /**
     * A document adapter
     */
    private static class DocumentAdapter extends PrintDocumentAdapter {
        private PrintAttributes mAttributes;
        private Paint mPaint;
        private RectF mTextBounds;
        private List<String> mLines;
        private List<String> mAdjustedLines;

        private static final int MILS_PER_INCH = 1000;
        private static final int POINTS_IN_INCH = 72;

        private final Context mCtx;
        private final FileSystemObject mDocument;
        private final int mPrintPageMargin;
        private final DocumentAdapterReader mReader;

        public DocumentAdapter(Context ctx, FileSystemObject document,
                DocumentAdapterReader reader) {
            super();
            mCtx = ctx;
            mDocument = document;
            mPrintPageMargin = ctx.getResources().getDimensionPixelSize(
                    R.dimen.print_page_margins);
            mReader = reader;
        }

        @Override
        public void onStart() {
            super.onStart();

            // Create the paint used for draw text
            Typeface courier = Typeface.createFromAsset(mCtx.getAssets(),
                    "fonts/Courier-Prime.ttf");
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setTypeface(courier);
            mPaint.setTextSize(mCtx.getResources().getDimensionPixelSize(
                    R.dimen.print_text_size));
            mPaint.setColor(Color.BLACK);

            // Get the text width and height
            mTextBounds = new RectF();
            mTextBounds.right = mPaint.measureText(new char[]{'A'}, 0, 1);
            mTextBounds.bottom = mPaint.getFontMetrics().descent
                    - mPaint.getFontMetrics().ascent + mPaint.getFontMetrics().leading;

            mLines = new ArrayList<String>();
            mAdjustedLines = new ArrayList<String>();
            mReader.read(mLines, mAdjustedLines);
        }

        @Override
        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                CancellationSignal cancellationSignal, WriteResultCallback callback) {
            PrintedPdfDocument pdfDocument = new PrintedPdfDocument(mCtx,
                    mAttributes);
            try {
                Rect pageContentRect = getContentRect(mAttributes);
                int charsPerRow = (int) (pageContentRect.width() / mTextBounds.width());
                int rowsPerPage = rowsPerPage(pageContentRect);

                int currentPage = 0;
                int currentLine = 0;
                Page page = null;
                if (mAdjustedLines.size() > 0) {
                    page = pdfDocument.startPage(currentPage++);
                    printHeader(mCtx, page, pageContentRect, charsPerRow);
                }
                // Top (with margin) + header
                float top = pageContentRect.top + (mTextBounds.height() * 2);
                for (String line : mAdjustedLines) {
                    currentLine++;
                    page.getCanvas().drawText(line, pageContentRect.left,
                            top + (currentLine * mTextBounds.height()), mPaint);

                    if (currentLine >= rowsPerPage) {
                        if (page != null) {
                            printFooter(mCtx, page, pageContentRect, currentPage);
                            pdfDocument.finishPage(page);
                        }
                        currentLine = 0;
                        page = pdfDocument.startPage(currentPage++);
                        printHeader(mCtx, page, pageContentRect, charsPerRow);
                    }
                }

                // Finish the last page
                if (page != null) {
                    printFooter(mCtx, page, pageContentRect, currentPage);
                    pdfDocument.finishPage(page);
                } else {
                    page = pdfDocument.startPage(1);
                    printHeader(mCtx, page, pageContentRect, charsPerRow);
                    printFooter(mCtx, page, pageContentRect, currentPage);
                    pdfDocument.finishPage(page);
                }

                try {
                    // Write the document
                    pdfDocument.writeTo(new FileOutputStream(destination.getFileDescriptor()));

                    // Done
                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                } catch (IOException ioe) {
                    // Failed.
                    ExceptionUtil.translateException(mCtx, ioe);
                    callback.onWriteFailed("Failed to print image");
                }
            } finally {
                if (destination != null) {
                    try {
                        destination.close();
                    } catch (IOException ioe) {
                        /* ignore */
                    }
                }
            }
        }

        @Override
        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                CancellationSignal cancellationSignal, LayoutResultCallback callback,
                Bundle extras) {

            // Check if document is valid
            if (mReader.getDocumentMode() == 0) {
                callback.onLayoutFailed("Failed to read document");
                return;
            }

            if (cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }
            mAttributes = newAttributes;
            Rect pageContentRect = getContentRect(newAttributes);
            int charsPerRow = (int) (pageContentRect.width() / mTextBounds.width());
            int rowsPerPage = rowsPerPage(pageContentRect);
            adjustLines(pageContentRect, charsPerRow);

            PrintDocumentInfo info = new PrintDocumentInfo.Builder(mDocument.getName())
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(calculatePageCount(rowsPerPage))
                .build();
            info.setDataSize(mDocument.getSize());
            boolean changed = !newAttributes.equals(oldAttributes);
            callback.onLayoutFinished(info, changed);
        }

        private Rect getContentRect(PrintAttributes attributes) {
            MediaSize mediaSize = attributes.getMediaSize();

            // Compute the size of the target canvas from the attributes.
            int pageWidth = (int) (((float) mediaSize.getWidthMils() / MILS_PER_INCH)
                    * POINTS_IN_INCH);
            int pageHeight = (int) (((float) mediaSize.getHeightMils() / MILS_PER_INCH)
                    * POINTS_IN_INCH);

            // Compute the content size from the attributes.
            Margins minMargins = attributes.getMinMargins();
            final int marginLeft = (int) (((float) minMargins.getLeftMils() / MILS_PER_INCH)
                    * POINTS_IN_INCH);
            final int marginTop = (int) (((float) minMargins.getTopMils() / MILS_PER_INCH)
                    * POINTS_IN_INCH);
            final int marginRight = (int) (((float) minMargins.getRightMils() / MILS_PER_INCH)
                    * POINTS_IN_INCH);
            final int marginBottom = (int) (((float) minMargins.getBottomMils() / MILS_PER_INCH)
                    * POINTS_IN_INCH);
            return new Rect(
                    Math.max(marginLeft, mPrintPageMargin),
                    Math.max(marginTop, mPrintPageMargin),
                    pageWidth - Math.max(marginRight, mPrintPageMargin),
                    pageHeight - Math.max(marginBottom, mPrintPageMargin));
        }

        private void printHeader(Context ctx, Page page, Rect pageContentRect,
                int charsPerRow) {
            String header = ctx.getString(R.string.print_document_header, mDocument.getName());
            if (header.length() >= charsPerRow) {
                header = header.substring(header.length() - 3) + "...";
            }
            page.getCanvas().drawText(header,
                    (int) (pageContentRect.width() / 2) - (mPaint.measureText(header) / 2),
                    pageContentRect.top + mTextBounds.height(), mPaint);
        }

        private void printFooter(Context ctx, Page page, Rect pageContentRect, int pageNumber) {
            String footer = ctx.getString(R.string.print_document_footer, pageNumber);
            page.getCanvas().drawText(footer,
                    (int) (pageContentRect.width() / 2) - (mPaint.measureText(footer) / 2),
                    pageContentRect.bottom - mTextBounds.height(), mPaint);
        }

        private void adjustLines(Rect pageRect, int charsPerRow) {
            if (mReader.getDocumentMode() == 2) {
                return;
            }
            mAdjustedLines = new ArrayList<String>(mLines);
            for (int i = 0; i < mAdjustedLines.size(); i++) {
                String line = mAdjustedLines.get(i);
                if (line.length() > charsPerRow) {
                    int prevSpace = line.lastIndexOf(" ", charsPerRow);
                    if (prevSpace != -1) {
                        // Split in the previous word
                        String currentLine = line.substring(0, prevSpace + 1);
                        String nextLine = line.substring(prevSpace + 1);
                        mAdjustedLines.set(i, currentLine);
                        mAdjustedLines.add(i + 1, nextLine);
                    } else {
                        // Just split at margin
                        String currentLine = line.substring(0, charsPerRow);
                        String nextLine = line.substring(charsPerRow);
                        mAdjustedLines.set(i, currentLine);
                        mAdjustedLines.add(i + 1, nextLine);
                    }
                }
            }
        }

        private int calculatePageCount(int rowsPerPage) {
            int pages = mAdjustedLines.size() / rowsPerPage;
            return pages <= 0 ? PrintDocumentInfo.PAGE_COUNT_UNKNOWN : pages;
        }

        private int rowsPerPage(Rect pageContentRect) {
            // Text height - header - footer
            return (int) ((pageContentRect.height() / mTextBounds.height()) - 4);
        }
    }

    /**
     * Method that prints the document from a string buffer
     *
     * @param ctx The current context
     * @param fso The document to print
     * @param sb The buffer to print
     * @param adjustLines If document must be adjusted
     */
    public static void printStringDocument(final Context ctx, final FileSystemObject document,
            final StringBuilder sb) {
        PrintManager printManager = (PrintManager) ctx.getSystemService(Context.PRINT_SERVICE);
        PrintAttributes attr = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .build();
        final DocumentAdapterReader reader = new DocumentAdapterReader() {
            @Override
            public void read(List<String> lines, List<String> adjustedLines) {
                BufferedReader br = null;
                try {
                    int bufferSize = ctx.getResources().getInteger(R.integer.buffer_size);
                    br = new BufferedReader(new StringReader(sb.toString()), bufferSize);
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        lines.add(line);
                    }

                } catch (IOException ex) {
                    Log.e(TAG, "Failed to read file " + document.getFullPath(), ex);
                    lines.clear();
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException ex) {
                            // Ignore
                        }
                    }
                }
            }

            @Override
            public int getDocumentMode() {
                // Always is text
                return 1;
            }
        };
        printManager.print(document.getName(), new DocumentAdapter(ctx, document, reader), attr);
    }

    /**
     * Method that prints the document as a text document
     *
     * @param ctx The current context
     * @param fso The document to print
     */
    private static void printTextDocument(final Context ctx, final FileSystemObject document) {
        PrintManager printManager = (PrintManager) ctx.getSystemService(Context.PRINT_SERVICE);
        PrintAttributes attr = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .build();
        final DocumentAdapterReader reader = new DocumentAdapterReader() {
            private int mDocumentMode = -1;

            @Override
            public void read(List<String> lines, List<String> adjustedLines) {
                mDocumentMode = getDocumentMode();
                if (mDocumentMode <= 0) {
                    lines.clear();
                } else if (mDocumentMode == 2) {
                    adjustedLines.addAll(readHexDumpDocumentFile(ctx, document, lines));
                } else {
                    readDocumentFile(ctx, document, lines);
                }
            }

            @Override
            public int getDocumentMode() {
                if (mDocumentMode == -1) {
                    String mimeType = MimeTypeHelper.getMimeType(ctx, document);
                    if (mimeType == null) {
                        mDocumentMode = 0; // Invalid
                    } else {
                        mDocumentMode = isBinaryDocument(ctx, document) ? 2 : 1; // binary / text
                    }
                }
                return mDocumentMode;
            }
        };
        printManager.print(document.getName(), new DocumentAdapter(ctx, document, reader), attr);
    }

    /**
     * Method that prints the document as a Pdf
     *
     * @param ctx The current context
     * @param fso The pdf to print
     */
    private static void printPdfDocument(final Context ctx, final FileSystemObject document) {
        PrintManager printManager = (PrintManager) ctx.getSystemService(Context.PRINT_SERVICE);
        PrintAttributes.MediaSize mediaSize = PrintAttributes.MediaSize.UNKNOWN_PORTRAIT;
        PrintAttributes attr = new PrintAttributes.Builder()
                .setMediaSize(mediaSize)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        printManager.print(document.getName(), new PrintDocumentAdapter() {
            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                    CancellationSignal cancellationSignal, WriteResultCallback callback) {
                FileInputStream fis = null;
                FileOutputStream fos = null;
                AsyncDocumentReader reader = null;

                try {
                    // Try first with java.io before using pipes

                    File file = new File(document.getFullPath());
                    if (file.isFile() && file.canRead()) {
                        fis = new FileInputStream(file);
                    } else {
                        reader = new AsyncDocumentReader(ctx);
                        CommandHelper.read(ctx, document.getFullPath(), reader, null);
                        fis = reader.mIn;
                    }
                    fos = new FileOutputStream(destination.getFileDescriptor());

                    // Write the document
                    int bufferSize = ctx.getResources().getInteger(R.integer.buffer_size);
                    byte[] data = new byte[bufferSize];
                    int read = 0;
                    while ((read = fis.read(data)) > 0) {
                        fos.write(data, 0, read);
                    }

                    // All was ok
                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

                } catch (Exception ex) {
                    // Failed.
                    ExceptionUtil.translateException(ctx, ex);
                    callback.onWriteFailed("Failed to print image");

                } finally {
                    try {
                        if (fis != null) {
                            fis.close();
                        }
                    } catch (IOException e) {
                        // Ignore
                    }
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        // Ignore
                    }
                    if (reader != null && reader.mIn != null) {
                        try {
                            reader.mIn.close();
                        } catch (IOException ex) {
                            //Ignore
                        }
                    }
                }
            }

            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                    CancellationSignal cancellationSignal, LayoutResultCallback callback,
                    Bundle extras) {

                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }

                PrintDocumentInfo info = new PrintDocumentInfo.Builder(document.getName())
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build();
                boolean changed = !newAttributes.equals(oldAttributes);
                callback.onLayoutFinished(info, changed);
            }
        }, attr);
    }

    /**
     * Method that prints the document as an image
     *
     * @param ctx The current context
     * @param fso The image to print
     */
    private static void printImage(final Context ctx, final FileSystemObject image) {
        // Check that the image is supported by Android
        if (isValidImageDocument(image.getFullPath())) {
            DialogHelper.showToast(ctx, R.string.print_unsupported_image, Toast.LENGTH_SHORT);
            return;
        }

        Bitmap bitmap = null;
        AsyncDocumentReader reader = null;
        try {
            // Try first with java.io before using pipes
            File file = new File(image.getFullPath());
            if (file.isFile() && file.canRead()) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmap = BitmapFactory.decodeFile(image.getFullPath(), options);
            } else {
                reader = new AsyncDocumentReader(ctx);
                CommandHelper.read(ctx, image.getFullPath(), reader, null);

                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                bitmap = BitmapFactory.decodeStream(reader.mIn);
            }
            if (bitmap == null) {
                throw new IOException("Failed to load image");
            }

        } catch (Exception ex) {
            ExceptionUtil.translateException(ctx, ex);
            return;

        } finally {
            if (reader != null && reader.mIn != null) {
                try {
                    reader.mIn.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
            if (reader != null && reader.mFdIn != null) {
                try {
                    reader.mFdIn.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
        }

        final Bitmap fBitmap = bitmap;
        PrintManager printManager = (PrintManager) ctx.getSystemService(Context.PRINT_SERVICE);
        PrintAttributes.MediaSize mediaSize = PrintAttributes.MediaSize.UNKNOWN_PORTRAIT;
        if (fBitmap.getWidth() > fBitmap.getHeight()) {
            mediaSize = PrintAttributes.MediaSize.UNKNOWN_LANDSCAPE;
        }
        PrintAttributes attr = new PrintAttributes.Builder()
                .setMediaSize(mediaSize)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        printManager.print(image.getName(), new PrintDocumentAdapter() {
            private PrintAttributes mAttributes;

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                    CancellationSignal cancellationSignal, WriteResultCallback callback) {
                PrintedPdfDocument pdfDocument = new PrintedPdfDocument(ctx,
                        mAttributes);
                try {
                    Page page = pdfDocument.startPage(1);
                    RectF content = new RectF(page.getInfo().getContentRect());
                    Matrix matrix = getMatrix(fBitmap.getWidth(), fBitmap.getHeight(), content);

                    // Draw the bitmap.
                    page.getCanvas().drawBitmap(fBitmap, matrix, null);

                    // Finish the page.
                    pdfDocument.finishPage(page);

                    try {
                        // Write the document
                        pdfDocument.writeTo(new FileOutputStream(destination.getFileDescriptor()));

                        // Done
                        callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                    } catch (IOException ioe) {
                        // Failed.
                        ExceptionUtil.translateException(ctx, ioe);
                        callback.onWriteFailed("Failed to print image");
                    }
                } finally {
                    if (pdfDocument != null) {
                        pdfDocument.close();
                    }
                    if (destination != null) {
                        try {
                            destination.close();
                        } catch (IOException ioe) {
                            /* ignore */
                        }
                    }
                }
            }

            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                    CancellationSignal cancellationSignal, LayoutResultCallback callback,
                    Bundle extras) {

                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }
                mAttributes = newAttributes;

                PrintDocumentInfo info = new PrintDocumentInfo.Builder(image.getName())
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                    .setPageCount(1)
                    .build();
                boolean changed = !newAttributes.equals(oldAttributes);
                callback.onLayoutFinished(info, changed);
            }

            @Override
            public void onFinish() {
                super.onFinish();
                if (fBitmap != null) {
                    fBitmap.recycle();
                }
            }

            private Matrix getMatrix(int imageWidth, int imageHeight, RectF content) {
                Matrix matrix = new Matrix();

                // Compute and apply scale to fill the page.
                float widthRatio = content.width() / imageWidth;
                float heightRatio = content.height() / imageHeight;
                float scale = Math.max(widthRatio, heightRatio);
                matrix.postScale(scale, scale);

                // Center the content.
                final float translateX = (content.width()
                        - imageWidth * scale) / 2;
                final float translateY = (content.height()
                        - imageHeight * scale) / 2;
                matrix.postTranslate(translateX, translateY);
                return matrix;
            }
        }, attr);
    }

    /**
     * Check if the file is a valid image document allowed by android to be printed
     *
     * @param file The image to check
     * @return boolean If the image is a valid document
     */
    private static boolean isValidImageDocument(String file) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(file, options);
        if (bitmap != null) {
            bitmap.recycle();
        }
        return bitmap != null;
    }

    /**
     * Method that checks if the file has a binary format
     *
     * @param ctx The current context
     * @param document The document to read
     * @return boolean If the document has a binary format
     */
    private static boolean isBinaryDocument(Context ctx, FileSystemObject document) {
        BufferedReader br = null;
        boolean binary = false;
        AsyncDocumentReader reader = null;
        try {
            reader = new AsyncDocumentReader(ctx);
            ReadExecutable command = CommandHelper.read(ctx, document.getFullPath(), reader, null);
            br = new BufferedReader(new InputStreamReader(reader.mIn));

            char[] data = new char[50];
            int read = br.read(data);
            for (int i = 0; i < read; i++) {
                if (!StringHelper.isPrintableCharacter(data[i])) {
                    binary = true;
                    break;
                }
            }
            command.cancel();

        } catch (Exception ex) {
            //Ignore
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
            if (reader != null && reader.mIn != null) {
                try {
                    reader.mIn.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
            if (reader != null && reader.mFdIn != null) {
                try {
                    reader.mFdIn.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
        }
        return binary;
    }

    /**
     * Read a file as document
     *
     * @param ctx The current context
     * @param document The document to read
     * @param lines The output
     */
    private static void readDocumentFile(Context ctx, FileSystemObject document,
            List<String> lines) {
        BufferedReader br = null;
        AsyncDocumentReader reader = null;
        try {
            // Async read the document while blocking with a buffered reader
            int bufferSize = ctx.getResources().getInteger(R.integer.buffer_size);
            reader = new AsyncDocumentReader(ctx);
            CommandHelper.read(ctx, document.getFullPath(), reader, null);
            br = new BufferedReader(new InputStreamReader(reader.mIn), bufferSize);

            String line = null;
            while((line = br.readLine()) != null) {
                lines.add(line);
            }

            // Got an exception?
            if (reader.mCause != null) {
                lines.clear();
                Log.e(TAG, "Failed to read file " + document.getFullPath(), reader.mCause);
            }

        } catch (Exception ex) {
            lines.clear();
            Log.e(TAG, "Failed to read file " + document.getFullPath(), ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
            if (reader != null && reader.mIn != null) {
                try {
                    reader.mIn.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
            if (reader != null && reader.mFdIn != null) {
                try {
                    reader.mFdIn.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
        }
    }

    /**
     * Read a file as hex document
     *
     * @param ctx The current context
     * @param document The document to read
     * @param lines The internal output
     * @return output The output
     */
    private static List<String> readHexDumpDocumentFile(Context ctx, FileSystemObject document,
            List<String> lines) {
        InputStream is = null;
        ByteArrayOutputStream baos;
        AsyncDocumentReader reader = null;
        try {
            // Async read the document while blocking with a buffered stream
            reader = new AsyncDocumentReader(ctx);
            CommandHelper.read(ctx, document.getFullPath(), reader, null);

            int bufferSize = ctx.getResources().getInteger(R.integer.buffer_size);
            baos = new ByteArrayOutputStream();
            is = new BufferedInputStream(reader.mIn);

            byte[] data = new byte[bufferSize];
            int read = 0;
            while((read = is.read(data, 0, bufferSize)) != -1) {
                baos.write(data, 0, read);
            }

            // Got an exception?
            if (reader.mCause != null) {
                lines.clear();
                Log.e(TAG, "Failed to read file " + document.getFullPath(), reader.mCause);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to read file " + document.getFullPath(), ex);
            lines.clear();
            return new ArrayList<String>();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
            if (reader != null && reader.mIn != null) {
                try {
                    reader.mIn.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
            if (reader != null && reader.mFdIn != null) {
                try {
                    reader.mFdIn.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
        }

        // Convert the bytes to a hex printable string and free resources
        String documentBuffer = StringHelper.toHexPrintableString(baos.toByteArray());
        try {
            baos.close();
        } catch (IOException ex) {
            //Ignore
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new StringReader(documentBuffer));
            String line = null;
            while((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException ex) {
            lines.clear();
            Log.e(TAG, "Failed to read file " + document.getFullPath(), ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
        }

        // Use the final array and clear the original (we don't use it anymore)
        List<String> output = new ArrayList<String>(lines);
        lines.clear();
        return output;
    }

    /**
     * An implementation of an {@code AsyncResultListener} based on pipes for readers
     */
    private static class AsyncDocumentReader implements AsyncResultListener {

        final FileInputStream mIn;
        private final FileOutputStream mOut;
        final ParcelFileDescriptor mFdIn;
        private final ParcelFileDescriptor mFdOut;
        Exception mCause;

        public AsyncDocumentReader(Context ctx) throws IOException {
            super();

            ParcelFileDescriptor[] fds = ParcelFileDescriptor.createReliablePipe();
            mFdIn = fds[0];
            mFdOut = fds[1];
            mIn = new ParcelFileDescriptor.AutoCloseInputStream(mFdIn);
            mOut = new ParcelFileDescriptor.AutoCloseOutputStream(mFdOut);
            mCause = null;
        }

        @Override
        public void onAsyncStart() {
            // Ignore
        }

        @Override
        public void onAsyncEnd(boolean cancelled) {
            // Ignore
        }

        @Override
        public void onAsyncExitCode(int exitCode) {
            close();
        }

        @Override
        public void onPartialResult(Object result) {
            try {
                if (result == null) return;
                byte[] partial = (byte[])result;
                mOut.write(partial);
                mOut.flush();
            } catch (Exception ex) {
                Log.w(TAG, "Failed to parse partial result data", ex);
                closeWithError("Failed to parse partial result data: " + ex.getMessage());
                mCause = ex;
            }
        }

        @Override
        public void onException(Exception cause) {
            Log.w(TAG, "Got exception while reading data", cause);
            closeWithError("Got exception while reading data: " + cause.getMessage());
            mCause = cause;
        }

        private void close() {
            try {
                mOut.close();
            } catch (IOException ex) {
                // Ignore
            }
            try {
                mFdOut.close();
            } catch (IOException ex) {
                // Ignore
            }
        }

        private void closeWithError(String msg) {
            try {
                mOut.close();
            } catch (IOException ex) {
                // Ignore
            }
            try {
                mIn.close();
            } catch (IOException ex) {
                // Ignore
            }
            try {
                mFdOut.closeWithError(msg);
            } catch (IOException ex) {
                // Ignore
            }
            try {
                mFdIn.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }
}
