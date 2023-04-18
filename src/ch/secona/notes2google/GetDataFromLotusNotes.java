package ch.secona.notes2google;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.mindoo.domino.jna.NotesCollection;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesViewEntryData;
import com.mindoo.domino.jna.constants.Navigate;
import com.mindoo.domino.jna.constants.ReadMask;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.utils.IDUtils;
import com.mindoo.domino.jna.utils.NotesInitUtils;
import com.mindoo.domino.jna.utils.StringUtil;

import lotus.domino.NotesThread;
import lotus.notes.NotesException;

/**
 * To run this standalone sample app:<br>
 * <ul>
 * <li>add the Notes.jar to the Java classpath</li>
 * <li>add the Notes/Domino program directory to the PATH, e.g.
 * "/Applications/IBM Notes.app/Contents/MacOS" on macOS or "C:\Program Files
 * (x86)\IBM\Notes" on Windows</li>
 * <li>on macOS: set the environment variable DYLD_LIBRARY_PATH to the same
 * value</li>
 * </ul>
 * Use these commandline parameters to launch:<br>
 * <br>
 * On macOS:<br>
 * <code>"-notesdir:/Applications/IBM Notes.app/Contents/MacOS" "-ini:/Users/klehmann/Library/Preferences/Notes Preferences"</code><br>
 * and on Windows:<br>
 * <code>"-notesdir:C:\Program Files (x86)\IBM\Notes" "-ini:C:\Program Files (x86)\IBM\Notes\Notes.ini"</code><br>
 * <br>
 * (change paths to match your Notes Client installation and Notes.ini path)<br>
 * <br>
 *
 * @author Karsten Lehmann
 */
public class GetDataFromLotusNotes {

	private static final String NAMES_NSF = "names.nsf";
	private static final String NOTES_USER = "Armin Kenel/Secona";
	private static final String PATH = "out/lotus-notes-data.json";

	private static String stripQuotes(String str) {
		if (str != null) {
			if (str.startsWith("\"")) {
				str = str.substring(1);
			}
			if (str.endsWith("\"")) {
				str = str.substring(0, str.length() - 1);
			}
		}
		return str;
	}

	public static void main(final String[] args) {
		System.out.println("Environment: " + System.getenv());
		System.out.println();
		System.out.println("PATH: " + System.getenv("PATH"));

		String notesProgramDirPath = null;
		String notesIniPath = null;

		// we support setting the notes program directory and notes.ini file path as
		// command line parameter
		// and environment variables
		for (final String currArg : args) {
			if (StringUtil.startsWithIgnoreCase(currArg, "-notesdir=")) {
				notesProgramDirPath = currArg.substring("-notesdir=".length());
			}
			if (StringUtil.startsWithIgnoreCase(currArg, "-ini=")) {
				notesIniPath = currArg.substring("-ini=".length());
			}
		}

		if (StringUtil.isEmpty(notesProgramDirPath)) {
			notesProgramDirPath = System.getenv("Notes_ExecDirectory");
		}

		if (StringUtil.isEmpty(notesIniPath)) {
			notesIniPath = System.getenv("NotesINI");
		}

		notesProgramDirPath = stripQuotes(notesProgramDirPath);
		notesIniPath = stripQuotes(notesIniPath);

		String[] notesInitArgs;
		if (!StringUtil.isEmpty(notesProgramDirPath) && !StringUtil.isEmpty(notesIniPath)) {
			notesInitArgs = new String[] { notesProgramDirPath, "=" + notesIniPath };
		} else {
			notesInitArgs = new String[0];
		}

		int exitStatus = 0;
		boolean notesInitialized = false;
		// call notesInitExtended on app startup
		try {
			System.out.println("Initializing Notes API with launch arguments: " + Arrays.toString(notesInitArgs));

			// System.setProperty("java.library.path", "C:\\Program Files
			// (x86)\\HCL\\Notes");
			System.setProperty("jna.library.path", "C:\\Program Files (x86)\\HCL\\Notes");
			System.setProperty("DYLD_LIBRARY_PATH", "C:\\Program Files (x86)\\HCL\\Notes");
			System.setProperty("LD_LIBRARY_PATH", "C:\\Program Files (x86)\\HCL\\Notes");

			NotesInitUtils.notesInitExtended(notesInitArgs);
			notesInitialized = true;

			final GetDataFromLotusNotes app = new GetDataFromLotusNotes();

			app.run();
		} catch (final NotesError e) {
			e.printStackTrace();
			exitStatus = -1;

			if (e.getId() == 421) {
				// 421 happens most of the time
				System.err.println();
				System.err.println(
						"Please make sure that the Notes.ini exists and specify Notes program dir and notes.ini path like this:");
				System.err.println("Mac:");
				System.err.println(
						"\"-notesdir=/Applications/IBM Notes.app/Contents/MacOS\" \"-ini:/Users/klehmann/Library/Preferences/Notes Preferences\"");
				System.err.println("Windows:");
				System.err.println(
						"\"-notesdir=C:\\Program Files (x86)\\IBM\\Notes\" \"-ini:C:\\Program Files (x86)\\IBM\\Notes\\Notes.ini\"");
				System.err.println();
				System.err.println(
						"As an alternative, use environment variables Notes_ExecDirectory and NotesINI for those two paths.");
			} else if (e.getId() == 258) {
				System.err.println();
				System.err.println(
						"If using macOS Catalina, make sure that the java process has full disk access rights in the"
								+ " macOS security settings. Looks like we cannot access the Notes directories.");
			} else {
				System.err.println();
				System.err.println("Notes init failed with error code " + e.getId());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			exitStatus = -1;
		} finally {
			if (notesInitialized) {
				NotesInitUtils.notesTerm();
			}
		}
		System.exit(exitStatus);
	}

	public void run() throws Exception {
		try {
			// initial Notes/Domino access for current thread (running single-threaded here)
			NotesThread.sinitThread();
			// launch run method within runWithAutoGC block to let it collect/dispose C
			// handles
			NotesGC.runWithAutoGC(() -> {
				// use IDUtils.switchToId if you want to unlock the ID file and switch the
				// current process
				// to this ID; should only be used in standalone applications
				// if this is missing, you will be prompted for your ID password the first time
				// the
				// id certs are required
				final String notesIdFilePath = System.getProperty("idfilepath");
				final String idPassword = System.getProperty("idpw");

				if (notesIdFilePath != null && notesIdFilePath.length() > 0 && idPassword != null
						&& idPassword.length() > 0) {
					// don't change Keyfileowner and other Notes.ini variables to this ID, so Notes
					// Client can
					// keep on running concurrently with his own ID
					final boolean dontSetEnvVar = true;

					IDUtils.switchToId(notesIdFilePath, idPassword, dontSetEnvVar);
				}
				// TODO
				// NotesGC.setPreferNotesTimeDate(true);
				readNotesDataAndWriteToFile();
				return null;
			});
		} finally {
			// terminate Notes/Domino access for current thread
			NotesThread.stermThread();
		}
	}

	private void readNotesDataAndWriteToFile() throws NotesException {
		System.out.println("Domino JNA test application");
		System.out.println("---------------------------");
		// System.out.println("Username of Notes ID: " + IDUtils.getIdUsername());

		final boolean isOnServer = IDUtils.isOnServer();

		System.out.println("Running on " + (isOnServer ? "server" : "client"));

		final String server = "";
		// final String filePath = "secona\\ak_names.nsf";
		final String filePath = NAMES_NSF;

		// empty string here opens the database as the Notes ID user, e.g. active Notes
		// Client user or the Domino server
		// final String openAsUser = "";

		// could as well be any other user; only works on local databases or between
		// machines listed in the "Trusted servers" list
		final String openAsUser = NOTES_USER;

		// open address book database
		final NotesDatabase dbNames = new NotesDatabase(server, filePath, openAsUser);
		// final Session session = NotesFactory.createSession();
		// final Database dbLegacyAPI = session.getDatabase(dbNames.getServer(),
		// dbNames.getRelativeFilePath());

		// open main view
		System.out.println("Reading People view");
		final NotesCollection peopleView = dbNames.openCollectionByName("My Contacts");
		// final NotesCollection peopleView = dbNames.openCollectionByName("People");

		// now start reading view data

		// "0" means one entry above the first row, not using "1" here, because that
		// entry could be
		// hidden by reader fields
		final String startPos = "0";
		// start with 1 here to move from the "0" position to the first document entry
		final int skipCount = 1;
		// NEXT_NONCATEGORY means only return document entries; use NEXT to read
		// categories as well
		// or NEXT_CATEGORY to return categories only
		final EnumSet<Navigate> navigationType = EnumSet.of(Navigate.NEXT_NONCATEGORY);

		// we want to read all view rows at once, use a lower value for web data, e.g.
		// just return 50 or 100 entries per request and use a paging grid in the UI
		final int count = Integer.MAX_VALUE;
		// since we want to read all view rows, fill the read buffer with all we can get
		// (max 64K)
		final int preloadEntryCount = Integer.MAX_VALUE;

		// decide which data to read; use SUMMARYVALUES to read column values;
		// use SUMMARYVALUES instead of SUMMARY to get more data into the buffer.
		// SUMMARY would not just return the column values but also the programmatic
		// column names, eating unnecessary buffer space
		// final EnumSet<ReadMask> readMask = EnumSet.of(ReadMask.NOTEUNID,
		// ReadMask.SUMMARYVALUES);
		final EnumSet<ReadMask> readMask = EnumSet.of(ReadMask.NOTEUNID, ReadMask.SUMMARYVALUES);

//		readMask.add(ReadMask.EXCLUDE_LEADING_PROGRAMMATIC_COLUMNS);
//		readMask.add(ReadMask.NOTEID);
//		readMask.add(ReadMask.ALL_TO_COLUMN);
//		readMask.add(ReadMask.SUMMARYVALUES);

		// if you are a more advanced user, take a look at the code behind the last
		// parameter; you don't have to work with NotesViewEntryData here, but
		// can produce your own application objects directly.
		final List<NotesViewEntryData> viewEntries = peopleView.getAllEntries(startPos, skipCount, navigationType,
				preloadEntryCount, readMask, new NotesCollection.EntriesAsListCallback(count));
		final List<InterfacePerson> interfacePersonList = new ArrayList<>();

		System.out.println("Read " + viewEntries.size() + " entries");

		for (final NotesViewEntryData currEntry : viewEntries) {
//			final NoteInfoExt ext = dbNames.getNoteInfoExt(currEntry.getNoteId());
//			final NotesNote notesNote = dbNames.openNoteByUnid(currEntry.getUNID());
//			final Document doc = notesNote.toDocument(dbLegacyAPI);
//
//			if (ext.exists()) {
//				final Document document = dbLegacyAPI.getDocumentByUNID(ext.getUnid());
//			}
			final InterfacePerson interfacePerson = mapInterfacePerson(currEntry);

			interfacePersonList.add(interfacePerson);
		}

		int numberOfPersons = 1;

		for (final InterfacePerson interfacePerson : interfacePersonList) {
			System.out.println(String.format("%4d: %s", numberOfPersons, interfacePerson));
			numberOfPersons++;
		}
		ReadWriteInterfacePerson.writeJson(PATH, interfacePersonList);
	}

	@SuppressWarnings("unchecked")
	private InterfacePerson mapInterfacePerson(final NotesViewEntryData currEntry) {
		final InterfacePerson interfacePerson = new InterfacePerson();
		final Map<String, Object> columnDataAsMap = currEntry.getColumnDataAsMap();

		final Object eMail = columnDataAsMap.get("$email");
		final Object phoneNumbers19 = columnDataAsMap.get("$19");
		final Object phoneNumbersEnglish = columnDataAsMap.get("$60");
		final Object addresses20 = columnDataAsMap.get("$20");
		final Object categories39 = columnDataAsMap.get("$39");
		final Object companyName = columnDataAsMap.get("companyname");
		final Object modified54 = columnDataAsMap.get("$54");
		final Object jobTitle = columnDataAsMap.get("jobtitle");
		final Object objectName126 = columnDataAsMap.get("$126");

		if (addresses20 != null && addresses20 instanceof String) {
			final String ad = (String) addresses20;

			if (!ad.isEmpty()) {
				System.err.println("Addresses ($20) is not null: " + columnDataAsMap);
			}
		}
		if (categories39 != null && categories39 instanceof String) {
			final String categoriesString = (String) categories39;

			interfacePerson.setCategories(categoriesString);
		}
		interfacePerson.setUid(currEntry.getUNID());
		if (eMail != null) {
			final String eMailString = (String) eMail;

			interfacePerson.seteMailBusiness(eMailString);
		}
		if (phoneNumbers19 != null) {
			if (phoneNumbers19 instanceof String) {
				final String phoneNumber = (String) phoneNumbers19;

				assignPhoneNumber(interfacePerson, phoneNumber);
			} else if (phoneNumbers19 instanceof List) {
				final List<String> phoneNumberList = (List<String>) phoneNumbers19;

				for (final String phoneNumber : phoneNumberList) {

					assignPhoneNumber(interfacePerson, phoneNumber);
				}
			}
		}
		if (phoneNumbersEnglish != null) {
			if (phoneNumbersEnglish instanceof String) {
				final String phoneNumber = (String) phoneNumbersEnglish;

				if (!phoneNumber.isEmpty()) {
					assignPhoneNumber(interfacePerson, phoneNumber);
				}
			} else if (phoneNumbersEnglish instanceof List) {
				final List<String> phoneNumberList = (List<String>) phoneNumbersEnglish;

				for (final String phoneNumber : phoneNumberList) {

					assignPhoneNumber(interfacePerson, phoneNumber);
				}
			}
		}
		if (companyName != null) {
			final String companyNameString = (String) companyName;

// for debugging purpose
//			if (companyNameString.contains("GVZ")) {
//				final int stop = 0;
//			}
			interfacePerson.setCompanyName(companyNameString);
		}
		if (modified54 != null) {
			if (modified54 instanceof GregorianCalendar) {
				final GregorianCalendar cal = (GregorianCalendar) modified54;
				final SimpleDateFormat formattedDate = new SimpleDateFormat("dd-MMM-yyyy");
				// use format() method to change the format
				final String dateFormatted = formattedDate.format(cal.getTime());

				interfacePerson.setModified(dateFormatted);
			}
		}
		if (jobTitle != null) {
			interfacePerson.setJobTitle((String) jobTitle);
		}
		if (objectName126 != null) {
			final String name = (String) objectName126;
			final int commaIndex = name.indexOf(',');

			if (commaIndex < 0) {
				interfacePerson.setLastName(name);
			} else {
				final String firstName = StringUtils.trim(name.substring(commaIndex + 1));
				final String lastName = StringUtils.trim(name.substring(0, commaIndex));

				interfacePerson.setFirstName(firstName);
				interfacePerson.setLastName(lastName);
			}
		}
		return interfacePerson;
	}

	private void assignPhoneNumber(final InterfacePerson interfacePerson, final String phoneNumber) {
		if (phoneNumber != null && !phoneNumber.isEmpty()) {
			final int indexOf = phoneNumber.indexOf(':');

			if (indexOf > -1) {
				final String type = phoneNumber.substring(0, indexOf);
				final String value = phoneNumber.substring(indexOf + 2);

				if ("Business".equals(type) || //
						"Office Phone".equals(type) || //
						"Office phone".equals(type)) {
					interfacePerson.setPhoneBusiness(value);
				} else if ("Business 2".equals(type)) {
					interfacePerson.setPhoneBusiness2(value);
				} else if ("Home".equals(type) || //
						"Home Phone".equals(type) || //
						"Home phone".equals(type)) {
					interfacePerson.setPhonePrivate(value);
				} else if ("Mobile".equals(type) || //
						"Mobile G".equals(type) || //
						"Mobile Business".equals(type) || //
						"Cellular".equals(type)) {
					final String mobileBusiness = interfacePerson.getMobileBusiness();

					if (mobileBusiness != null && !mobileBusiness.isEmpty()) {
						if (!mobileBusiness.equals(value)) {
							System.err.println("Mobile business is not equal");
						}
					}
					interfacePerson.setMobileBusiness(value);
				} else if ("Mobile 2 (Business)".equals(type)) {
					interfacePerson.setMobileBusiness2(value);
				} else if ("Cell Phone".equals(type) || //
						"Cell phone".equals(type) || //
						"Mobile Private".equals(type) || //
						"Mobile P".equals(type)) {
					final String mobilePrivate = interfacePerson.getMobilePrivate();

					if (mobilePrivate != null && !mobilePrivate.isEmpty()) {
						if (!mobilePrivate.equals(value)) {
							System.err.println("Mobile private is not equal");
						}
					}
					interfacePerson.setMobilePrivate(value);
				} else if ("Mobile 2".equals(type)) {
					interfacePerson.setMobilePrivate2(value);
				} else if ("Home Fax".equals(type) || //
						"Home fax".equals(type)) {
					interfacePerson.setFaxPrivate(value);
				} else if ("Notfallnummer".equals(type)) {
					interfacePerson.setEmergencyNumber(value);
				} else if ("Business Direct".equals(type)) {
					interfacePerson.setPhoneBusinessDirect(value);
				} else if ("Fax".equals(type) || //
						"Office Fax".equals(type) || //
						"Office fax".equals(type)) {
					final String faxBusiness = interfacePerson.getFaxBusiness();

					if (faxBusiness != null && !faxBusiness.isEmpty()) {
						if (!faxBusiness.equals(value)) {
							System.err.println("fax business not equal");
						}
					}
					interfacePerson.setFaxBusiness(value);
				} else {
					System.err.println("Unknown phone type: " + type);
				}
			}
		}
	}
}