package com.erakk.lnreader.parser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

import org.jsoup.nodes.Document;

import android.preference.PreferenceManager;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.LNReaderApplication;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.PageModel;

public class CommonParser {

	private static final String TAG = CommonParser.class.toString();

	/**
	 * Set Up image path
	 * 
	 * @param content
	 * @return
	 */
	public static String replaceImagePath(String content) {
		String imagePath = "src=\"file://" + UIHelper.getImageRoot(LNReaderApplication.getInstance().getApplicationContext()) + "/project/images/";
		content = content.replace("src=\"/project/images/", imagePath);
		return content;
	}

	/**
	 * Sanitizes a title by removing unnecessary stuff.
	 * 
	 * @param title
	 * @return
	 */
	public static String sanitize(String title, boolean isAggresive) {
		Log.d(TAG, "Before: " + title);
		title = title.replaceAll("<.+?>", "") // Strip tags
				.replaceAll("\\[.+?\\]", "") // Strip [___]s
				.replaceAll("- PDF", "").replaceAll("\\(PDF\\)", "") // Strip (PDF)
				// Strip - (Full Text)
				.replaceAll("- (Full Text)", "").replaceAll("- \\(.*Full Text.*\\)", "").replace("\\(.*Full Text.*\\)", "");
		Log.d(TAG, "After: " + title);
		if (isAggresive) {
			if (PreferenceManager.getDefaultSharedPreferences(LNReaderApplication.getInstance().getApplicationContext()).getBoolean(Constants.PREF_AGGRESIVE_TITLE_CLEAN_UP, true)) {
				// Leaves only the text before brackets (might be a bit too aggressive)
				title = title.replaceAll("^(.+?)[(\\[].*$", "$1");
				Log.d(TAG, "After Aggresive: " + title);
			}
		}
		return title.trim();
	}

	/**
	 * Remove redlink, user, and ISBN page
	 * 
	 * @param book
	 * @return
	 */
	public static ArrayList<PageModel> validateNovelChapters(BookModel book) {
		ArrayList<PageModel> chapters = book.getChapterCollection();
		ArrayList<PageModel> validatedChapters = new ArrayList<PageModel>();
		int chapterOrder = 0;
		for (Iterator<PageModel> iChapter = chapters.iterator(); iChapter.hasNext();) {
			PageModel chapter = iChapter.next();

			if (!(chapter.getPage().contains("redlink=1") || // missing page
					chapter.getPage().contains("User:") || // user page
			chapter.getPage().contains("Special:BookSources") // ISBN handler
			)) {
				chapter.setOrder(chapterOrder);
				validatedChapters.add(chapter);
				++chapterOrder;
			}
		}
		return validatedChapters;
	}

	/**
	 * Remove invalid chapter from volumes
	 * 
	 * @param books
	 * @return
	 */
	public static ArrayList<BookModel> validateNovelBooks(ArrayList<BookModel> books) {
		ArrayList<BookModel> validatedBooks = new ArrayList<BookModel>();
		int bookOrder = 0;
		for (Iterator<BookModel> iBooks = books.iterator(); iBooks.hasNext();) {
			BookModel book = iBooks.next();
			BookModel validatedBook = new BookModel();

			ArrayList<PageModel> validatedChapters = validateNovelChapters(book);

			// check if have any chapters
			if (validatedChapters.size() > 0) {
				validatedBook = book;
				validatedBook.setChapterCollection(validatedChapters);
				validatedBook.setOrder(bookOrder);
				validatedBooks.add(validatedBook);
				// Log.d("validateNovelBooks", "Adding: " + validatedBook.getTitle() + " order: " +
				// validatedBook.getOrder());
				++bookOrder;
			}
		}
		return validatedBooks;
	}

	/**
	 * Check if the page is redirected. Return null if not.
	 * 
	 * @param doc
	 * @param page
	 * @return
	 */
	public static String redirectedFrom(Document doc, PageModel page) {
		if (page.getRedirectedTo() != null) {
			try {
				return URLEncoder.encode(page.getRedirectedTo().replace(" ", "_"), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "Error when encoding redirected pages", e);
				return null;
			}
		}
		return null;
	}
}