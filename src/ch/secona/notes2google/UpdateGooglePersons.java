package ch.secona.notes2google;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.Credentials;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.people.v1.model.Address;
import com.google.api.services.people.v1.model.Biography;
import com.google.api.services.people.v1.model.Birthday;
import com.google.api.services.people.v1.model.ClientData;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Organization;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import com.google.api.services.people.v1.model.Relation;
import com.google.api.services.people.v1.model.Url;
import com.google.common.base.Strings;
import com.mindoo.domino.jna.utils.StringUtil;

public class UpdateGooglePersons {
	private static final String DD_MMM_YYYY_HH_MM_SS = "dd-MMM-yyyy HH:mm:ss";
	private static final String DD_MMM_YYYY = "dd-MMM-yyyy";
	private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat(DD_MMM_YYYY_HH_MM_SS);
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DD_MMM_YYYY);
	private static final String PATH = "out/lotus-notes-data.json";
	private static final String APPLICATION_NAME = UpdateGooglePersons.class.getSimpleName();
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";
	private static final String PEOPLE_ME = "people/me";
	private static final String PERSON_ATTRIBUTES = //
			"addresses," + //
					"ageRanges," + //
					"biographies," + //
					"birthdays," + //
					"calendarUrls," + //
					"clientData," + //
					"coverPhotos," + //
					"emailAddresses," + //
					"events," + //
					"externalIds," + //
					"genders," + //
					"imClients," + //
					"interests," + //
					"locales," + //
					"locations," + //
					"memberships," + //
					"metadata," + //
					"miscKeywords," + //
					"names," + //
					"nicknames," + //
					"occupations," + //
					"organizations," + //
					"phoneNumbers," + //
					"photos," + //
					"relations," + //
					"sipAddresses," + //
					"skills," + //
					"urls," + //
					"userDefined";
	private static final String UPDATE_PERSON_ATTRIBUTES = //
			"addresses," + //
					"biographies," + //
					"birthdays," + //
					"calendarUrls," + //
					"clientData," + //
					"emailAddresses," + //
					"events," + //
					"externalIds," + //
					"genders," + //
					"imClients," + //
					"interests," + //
					"locales," + //
					"locations," + //
					"memberships," + //
					"miscKeywords," + //
					"names," + //
					"nicknames," + //
					"occupations," + //
					"organizations," + //
					"phoneNumbers," + //
					"relations," + //
					"sipAddresses," + //
					"skills," + //
					"urls," + //
					"userDefined";
	// Global instance of the scopes required by this quick start. If modifying
	// these scopes, delete your previously saved tokens / folder.
	private static final List<String> SCOPES = Arrays.asList(PeopleServiceScopes.CONTACTS);
	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
	private static final String LOTUS_CONTACT_UNID = "LotusContactUnid";
	private static final String LOTUS_CONTACT_CATEGORIES = "LotusContactCategories";
	private static final String LOTUS_CONTACT_MODIFIED = "LotusContactModified";
	private static PeopleService peopleService;
	private static boolean verbose = false;

	/**
	 * Creates an authorized {@link Credentials} object.
	 *
	 * @param HTTP_TRANSPORT The network HTTP Transport (see
	 *                       {@link NetHttpTransport})
	 * @return An authorized {@link Credentials} object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		final InputStream in = UpdateGooglePersons.class.getResourceAsStream(CREDENTIALS_FILE_PATH);

		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}

		final GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
		// Build flow and trigger user authorization request.
		final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
				.setAccessType("offline").build();
		final LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	public static PeopleService getPeopleService() throws IOException, GeneralSecurityException {
		// build a new authorized API client service
		if (peopleService == null) {
			final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

			peopleService = new PeopleService.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME).build();
		}
		return peopleService;
	}

	public static void main(final String... args) throws IOException, GeneralSecurityException {

		for (final String currArg : args) {
			if (StringUtil.startsWithIgnoreCase(currArg, "-verbose")) {
				verbose = true;
			}
		}
		displayLine();
		displayText(APPLICATION_NAME);
		displayText(getNow());
		displayLine();
		displayText("");
		displayText("Reading all persons from Google ...");

		final List<Person> allPersons = getAllPersons(getPeopleService());

		displayText("Number of persons from Google: " + allPersons.size());
		displayText("Reading all people from Notes (interface file) ...");

		final List<InterfacePerson> interfacePersonList = ReadWriteInterfacePerson.readJson(PATH,
				InterfacePerson[].class);

		displayText("Number of people from Notes (interface file): " + interfacePersonList.size());
		displayText("");
		displayLine();
		displayText("");
		displayText("Persons in Google but not found in Notes:");
		showPersonsNotInNotes(allPersons, interfacePersonList);
		displayText("");
		displayLine();
		displayText("");
		displayText("People found in Notes but not found in Google (will be added or updated):");
		showPersonsNotInGoogleAndAddOrUpdateInGoogle(interfacePersonList, allPersons);
		displayText("");
		displayLine();
		displayText("");
		displayText("NotesId is null in Google:");
		showNotesIdInGoogleIsNull(allPersons);
		displayText("");
		displayLine();
		displayText("");
		displayText("Show all persons from Google:");

		final List<Person> personList = allPersons;
		int numberOfPersons;

		if (personList != null && personList.size() > 0) {
			numberOfPersons = 0;
			for (final Person person : personList) {
				numberOfPersons++;
				if (verbose) {
					showData(numberOfPersons, person);
				}
			}
			if (!verbose) {
				displayText("Number of persons in Google read: " + numberOfPersons);
			}
		} else {
			displayText("No persons in Google found.");
		}
		displayText("");
		displayLine();
		displayText(getNow());
		displayText("Terminated.");
		displayLine();
	}

	private static void showNotesIdInGoogleIsNull(final List<Person> allPersons) {
		allPersons.stream().forEach(p -> {
			final String lotusContactUnid = getLotusContactUnid(p);

			if (Strings.isNullOrEmpty(lotusContactUnid)) {
				displayError("NotesId is null: " + p.getNames());
			}
		});
	}

	private static void showPersonsNotInGoogleAndAddOrUpdateInGoogle( //
			final List<InterfacePerson> interfacePersonList, final List<Person> allGooglePersons) {
		int notFoundInGoogleCounter;
		int doUpdateCounter = 0;
		String lotusContactModified = "";
		Person foundGooglePerson = null;

		notFoundInGoogleCounter = 0;
		// get each person from the interface list (file)
		for (final InterfacePerson interfacePerson : interfacePersonList) {
			final String notesId = interfacePerson.getUid();

			foundGooglePerson = null;
			lotusContactModified = "";
			// we're looking for the Lotus Notes person
			// in the list of all Google persons
			for (final Person googlePerson : allGooglePersons) {
				if (getLotusContactUnid(googlePerson).equalsIgnoreCase(notesId)) {
					// person found
					foundGooglePerson = googlePerson;
					// remember the modification date
					lotusContactModified = getLotusContactModified(googlePerson);
				}
			}
			if (foundGooglePerson != null) {
				// update
				boolean doUpdate = false;
				String interfacePersonModified = interfacePerson.getModified();
				Date interfacePersonModifiedDate = string2Date(interfacePersonModified);
				Date lotusContactModifiedDate = string2Date(lotusContactModified);

				if (interfacePersonModifiedDate == null) {
					displayError("No modification date in Lotus Notes set - skip process for " + //
							interfacePerson.getShortInfo());
				} else {
					if (lotusContactModifiedDate == null) {
						displayText("No modification date in Google set - update " + //
								interfacePerson.getShortInfo());
						doUpdate = true;
					} else {
						if (interfacePersonModifiedDate.after(lotusContactModifiedDate)) {
							displayText("Lotus Notes update is newer - update " + //
									interfacePerson.getShortInfo() + //
									" - " + interfacePersonModified + //
									" / " + lotusContactModified);
							doUpdate = true;
						}
					}
					if (doUpdate) {
						doUpdateCounter++;
						// TODO: used for "filtering"
						// if (interfacePerson.getLastName().equals("Zoro")) {
						updateNotes2Google(doUpdateCounter, foundGooglePerson, interfacePerson);
						// }
					}
				}
			} else {
				notFoundInGoogleCounter++;
				addNotesToGoogle(interfacePerson, notFoundInGoogleCounter);
			}
		}
		displayLine();
		displayText("");
		displayText("statistic");
		displayText("---------");
		displayText("processed        : " + String.format("%4d", interfacePersonList.size()));
		displayText("added to Google  : " + String.format("%4d", notFoundInGoogleCounter));
		displayText("updated in Google: " + String.format("%4d", doUpdateCounter));
		displayText("");
		displayLine();
	}

	private static void updateNotes2Google(int doUpdateCounter, Person foundGooglePerson,
			final InterfacePerson interfacePerson) {
		final String resourceName = foundGooglePerson.getResourceName();

		fillGooglePerson(interfacePerson, foundGooglePerson);
		displayText("Update: " + interfacePerson.getShortInfo());
		try {
			final Person updatedPerson = getPeopleService().people().updateContact(resourceName, foundGooglePerson)
					.setUpdatePersonFields(UPDATE_PERSON_ATTRIBUTES).execute();

			updatedPerson.getResourceName();
		} catch (IOException | GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	private static void addNotesToGoogle(final InterfacePerson interfacePerson, final int count) {
		final Person personToCreate = new Person();

		fillGooglePerson(interfacePerson, personToCreate);
		displayText("Add: " + interfacePerson.getShortInfo());
		try {
			final Person createdPerson = getPeopleService().people().createContact(personToCreate).execute();

			createdPerson.getResourceName();
		} catch (final IOException | GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	private static Date string2Date(String stringDate) {
		Date date = null;

		if (!Strings.isNullOrEmpty(stringDate)) {
			try {
				date = DATE_TIME_FORMAT.parse(stringDate);
			} catch (final ParseException e) {
				// date/time format doesn't work
				try {
					// try (short) date format
					date = DATE_FORMAT.parse(stringDate);
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
			}
		}
		return date;
	}

	private static void fillGooglePerson(final InterfacePerson interfacePerson, final Person person) {

		fillNames(interfacePerson, person);
		fillPhones(interfacePerson, person);
		fillEmails(interfacePerson, person);
		fillAddresses(interfacePerson, person);
		fillRelations(interfacePerson, person);
		fillWebsites(interfacePerson, person);
		fillBirthdays(interfacePerson, person);
		fillOrganizations(interfacePerson, person);
		fillBiographies(interfacePerson, person);
		fillClientData(interfacePerson, person);
	}

	private static void fillNames(final InterfacePerson interfacePerson, final Person person) {
		final String firstName = interfacePerson.getFirstName();
		final String middleName = interfacePerson.getMiddleName();
		final String lastName = interfacePerson.getLastName();
		List<Name> nameList = person.getNames();

		if (nameList == null) {
			nameList = new ArrayList<Name>();
		} else {
			nameList.clear();
		}
		nameList.add(new Name().setGivenName(firstName).setFamilyName(lastName).setMiddleName(middleName));
		person.setNames(nameList);
	}

	private static void fillPhones(final InterfacePerson interfacePerson, final Person person) {
		final String phoneBusiness = interfacePerson.getPhoneBusiness();
		final String phoneBusinessDirect = interfacePerson.getPhoneBusinessDirect();
		final String phoneBusiness2 = interfacePerson.getPhoneBusiness2();
		final String mobileBusiness = interfacePerson.getMobileBusiness();
		final String mobileBusiness2 = interfacePerson.getMobileBusiness2();
		final String mobilePrivate = interfacePerson.getMobilePrivate();
		final String mobilePrivate2 = interfacePerson.getMobilePrivate2();
		final String phonePrivate = interfacePerson.getPhonePrivate();
		final String faxPrivate = interfacePerson.getFaxPrivate();
		final String faxBusiness = interfacePerson.getFaxBusiness();
		// final String emergencyNumber = p.getEmergencyNumber();
		List<PhoneNumber> phoneNumberList = person.getPhoneNumbers();

		if (phoneNumberList == null) {
			phoneNumberList = new ArrayList<PhoneNumber>();
		} else {
			phoneNumberList.clear();
		}
		// predefined values:
		// 'home'
		// 'work'
		// 'mobile'
		// 'homeFax'
		// 'workFax'
		// 'otherFax'
		// 'pager'
		// 'workMobile'
		// 'workPager'
		// 'main'
		// 'googleVoice'
		// 'other'
		// or null
		phoneNumberList.add(new PhoneNumber().setType("home").setValue(phonePrivate));
		phoneNumberList.add(new PhoneNumber().setType("work").setValue(phoneBusiness));
		phoneNumberList.add(new PhoneNumber().setType("mobile").setValue(mobilePrivate));
		phoneNumberList.add(new PhoneNumber().setType("homeFax").setValue(faxPrivate));
		phoneNumberList.add(new PhoneNumber().setType("workFax").setValue(faxBusiness));
		phoneNumberList.add(new PhoneNumber().setType("otherFax").setValue(""));
		phoneNumberList.add(new PhoneNumber().setType("pager").setValue(""));
		phoneNumberList.add(new PhoneNumber().setType("workMobile").setValue(mobileBusiness));
		phoneNumberList.add(new PhoneNumber().setType("workPager").setValue(""));
		phoneNumberList.add(new PhoneNumber().setType("main").setValue(""));
		phoneNumberList.add(new PhoneNumber().setType("googleVoice").setValue(""));
		phoneNumberList.add(new PhoneNumber().setType("other").setValue(""));
		// others
		phoneNumberList.add(new PhoneNumber().setType("work2").setValue(phoneBusiness2));
		phoneNumberList.add(new PhoneNumber().setType("workDirect").setValue(phoneBusinessDirect));
		phoneNumberList.add(new PhoneNumber().setType("workMobile2").setValue(mobileBusiness2));
		phoneNumberList.add(new PhoneNumber().setType("mobile2").setValue(mobilePrivate2));
		person.setPhoneNumbers(phoneNumberList);
	}

	private static void fillEmails(final InterfacePerson interfacePerson, final Person person) {
		final String eMailBusiness = interfacePerson.geteMailBusiness();
		final String eMailPrivate = interfacePerson.geteMailPrivate();
		List<EmailAddress> emailAddressList = person.getEmailAddresses();

		if (emailAddressList == null) {
			emailAddressList = new ArrayList<EmailAddress>();
		} else {
			emailAddressList.clear();
		}
		// predefined values:
		// 'home'
		// 'work'
		// 'other'
		// or null
		emailAddressList.add(new EmailAddress().setType("home").setValue(eMailPrivate));
		emailAddressList.add(new EmailAddress().setType("work").setValue(eMailBusiness));
		person.setEmailAddresses(emailAddressList);
	}

	private static void fillAddresses(final InterfacePerson interfacePerson, final Person person) {
		final String streetAddress = interfacePerson.getStreetAddress();
		final String zip = interfacePerson.getZip();
		final String city = interfacePerson.getCity();
		final String state = interfacePerson.getState();
		final String country = interfacePerson.getCountry();
		final String officeStreetAddress = interfacePerson.getOfficeStreetAddress();
		final String officeZip = interfacePerson.getOfficeZip();
		final String officeCity = interfacePerson.getOfficeCity();
		final String officeState = interfacePerson.getOfficeState();
		final String officeCountry = interfacePerson.getOfficeCountry();
		List<Address> addressList = person.getAddresses();

		if (addressList == null) {
			addressList = new ArrayList<Address>();
		} else {
			addressList.clear();
		}

		final Address privateAddress = new Address();
		final Address officeAddress = new Address();

		privateAddress.setType("home");
		privateAddress.setStreetAddress(streetAddress);
		privateAddress.setPostalCode(zip);
		privateAddress.setCity(city);
		privateAddress.setRegion(state);
		privateAddress.setCountry(country);
		officeAddress.setType("work");
		officeAddress.setStreetAddress(officeStreetAddress);
		officeAddress.setPostalCode(officeZip);
		officeAddress.setCity(officeCity);
		officeAddress.setRegion(officeState);
		officeAddress.setCountry(officeCountry);
		addressList.add(privateAddress);
		addressList.add(officeAddress);
		person.setAddresses(addressList);
	}

	private static void fillRelations(final InterfacePerson interfacePerson, final Person person) {
		final String children = interfacePerson.getChildren();
		final String spouse = interfacePerson.getSpouse();
		List<Relation> relationList = person.getRelations();

		if (relationList == null) {
			relationList = new ArrayList<Relation>();
		} else {
			relationList.clear();
		}
		relationList.add(new Relation().setType("child").setPerson(children));
		relationList.add(new Relation().setType("spouse").setPerson(spouse));
		person.setRelations(relationList);
	}

	private static void fillWebsites(final InterfacePerson interfacePerson, final Person person) {
		final String webSite = interfacePerson.getWebSite();
		List<Url> urlList = person.getUrls();

		if (urlList == null) {
			urlList = new ArrayList<Url>();
		} else {
			urlList.clear();
		}
		urlList.add(new Url().setType("work").setValue(webSite));
		person.setUrls(urlList);
	}

	private static void fillBirthdays(final InterfacePerson interfacePerson, final Person person) {
		final String birthDate = interfacePerson.getBirthDate();
		List<Birthday> birthdayList = person.getBirthdays();

		if (birthdayList == null) {
			birthdayList = new ArrayList<Birthday>();
		} else {
			birthdayList.clear();
		}
		birthdayList.add(new Birthday().setText(birthDate));
		person.setBirthdays(birthdayList);
	}

	private static void fillOrganizations(final InterfacePerson interfacePerson, final Person person) {
		final String company = interfacePerson.getCompanyName();
		final String jobTitle = interfacePerson.getJobTitle();
		List<Organization> organizationList = person.getOrganizations();

		if (organizationList == null) {
			organizationList = new ArrayList<Organization>();
		} else {
			organizationList.clear();
		}
		// predefined values:
		// 'work'
		// 'school'
		// or null
		organizationList.add(new Organization().setType("work").setName(company).setTitle(jobTitle));
		person.setOrganizations(organizationList);
	}

	private static void fillBiographies(final InterfacePerson interfacePerson, final Person person) {
		final String comment = interfacePerson.getComment();
		List<Biography> biographyList = person.getBiographies();

		if (biographyList == null) {
			biographyList = new ArrayList<Biography>();
		} else {
			biographyList.clear();
		}
		biographyList.add(new Biography().setValue(comment));
		person.setBiographies(biographyList);
	}

	private static void fillClientData(final InterfacePerson interfacePerson, final Person person) {
		final String id = interfacePerson.getUid();
		final String categories = interfacePerson.getCategories();
		final String modified = interfacePerson.getModified();
		List<ClientData> clientDataList = person.getClientData();

		if (clientDataList == null) {
			clientDataList = new ArrayList<>();
		} else {
			clientDataList.clear();
		}
		// represents "own" data, specially the Notes ID
		// for ensure correct synchronization
		clientDataList.add(new ClientData().setKey(LOTUS_CONTACT_UNID).setValue(id));
		clientDataList.add(new ClientData().setKey(LOTUS_CONTACT_CATEGORIES).setValue(categories));
		clientDataList.add(new ClientData().setKey(LOTUS_CONTACT_MODIFIED).setValue(modified));
		person.setClientData(clientDataList);
	}

	private static void showPersonsNotInNotes(final List<Person> allPersons,
			final List<InterfacePerson> interfacePersonList) {
		int notFoundInNotesCounter = 0;

		checkDoubleNotesIds(allPersons);
		for (final Person p : allPersons) {
			boolean found = false;
			final String lotusContactUnid = getLotusContactUnid(p);

			if (StringUtils.isEmpty(lotusContactUnid)) {
				displayError("Lotus Contact Unid is empty: " + p);
			}
			for (final InterfacePerson ip : interfacePersonList) {

				if (ip.getUid().equalsIgnoreCase(lotusContactUnid)) {
					found = true;
				}
			}
			if (!found) {
				notFoundInNotesCounter++;
				displayError(String.format("%4d. Not found in Notes: %s", //
						notFoundInNotesCounter, p.getNames()));
			}
		}
	}

	private static void checkDoubleNotesIds(final List<Person> allPersons) {

		final Map<String, String> map = new HashMap<>();

		for (final Person p : allPersons) {
			final String lotusContactUnid = getLotusContactUnid(p);
			final String value = map.get(lotusContactUnid);

			if (value == null) {
				map.put(lotusContactUnid, lotusContactUnid);
			} else {
				displayError("Notes Id already found: " + lotusContactUnid + " / " + p);
			}
		}
	}

	private static void showData(final int count, final Person person) {
		final List<Name> names = person.getNames();
		final String lotusContactUnid = getLotusContactUnid(person);
		final String email = getEmail(person);

		if (names != null && names.size() > 0) {
			displayText(String.format("%4d. [%s", count, names.get(0).getDisplayName() + //
					", notesId: " + lotusContactUnid + //
					", mail: " + email + "]"));
		} else {
			displayText(String.format("%4d. --> [%s]", count, person));
		}
	}

	private static String getLotusContactUnid(final Person person) {
		final List<ClientData> clientData = person.getClientData();
		String lotusContactUnid = "";

		if (clientData != null) {
			for (final ClientData clientDataItem : clientData) {
				final String key = clientDataItem.getKey();

				if (LOTUS_CONTACT_UNID.equals(key)) {
					lotusContactUnid = clientDataItem.getValue();
					// split
					final int indexOf = lotusContactUnid.indexOf('-');

					if (indexOf >= 0) {
						lotusContactUnid = lotusContactUnid.substring(indexOf + 1);
					}
				}
			}
		}
		return lotusContactUnid;
	}

	private static String getLotusContactModified(final Person person) {
		final List<ClientData> clientData = person.getClientData();
		String lotusContactModified = "";

		if (clientData != null) {
			for (final ClientData clientDataItem : clientData) {
				final String key = clientDataItem.getKey();

				if (LOTUS_CONTACT_MODIFIED.equals(key)) {
					lotusContactModified = clientDataItem.getValue();
				}
			}
		}
		return lotusContactModified;
	}

	private static String getEmail(final Person person) {
		final List<EmailAddress> emailAddresses = person.getEmailAddresses();
		String email = "";

		if (emailAddresses != null) {
			for (final EmailAddress emailAddress : emailAddresses) {
				final String type = emailAddress.getType();
				final String value = emailAddress.getValue();

				if (type == null) {
					displayError("Type of mail adress is null: " + value);
				} else if ("work".equals(type) || "WORK".equals(type)) {
					email = "work  = " + value;
				} else if ("home".equals(type) || "HOME".equals(type)) {
					email = "home  = " + value;
				} else if ("other".equals(type) || "OTHER".equals(type)) {
					email = "other = " + value;
				} else {
					throw new IllegalArgumentException("unknown type: " + type + " // " + person);
				}
			}
		}
		return email;
	}

	/**
	 * Read all persons using paging.
	 *
	 * @param peopleService
	 * @return a {@link List} of all {@link Person} objects
	 * @throws IOException
	 */
	private static List<Person> getAllPersons(final PeopleService peopleService) throws IOException {
		final List<Person> allPersons = new ArrayList<>();
		ListConnectionsResponse fullSyncResponse = peopleService. //
				people(). //
				connections(). //
				list(PEOPLE_ME). //
				setPersonFields(PERSON_ATTRIBUTES). //
				setRequestSyncToken(true). //
				execute();

		allPersons.addAll(fullSyncResponse.getConnections());
		// fetch all the pages
		while (fullSyncResponse.getNextPageToken() != null) {
			fullSyncResponse = peopleService. //
					people(). //
					connections(). //
					list(PEOPLE_ME). //
					setPersonFields(PERSON_ATTRIBUTES). //
					setRequestSyncToken(true). //
					setPageToken(fullSyncResponse.getNextPageToken()). //
					execute();
			allPersons.addAll(fullSyncResponse.getConnections());
		}
		return allPersons;
	}

	private static void displayText(final String text) {
		System.out.println(text);
	}

	private static void displayLine() {
		System.out.println("-----------------------------------------------------------------------");
	}

	private static void displayError(final String text) {
		System.err.println(text);
	}

	private static String getNow() {
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		final LocalDateTime now = LocalDateTime.now();

		return dtf.format(now);
	}
}