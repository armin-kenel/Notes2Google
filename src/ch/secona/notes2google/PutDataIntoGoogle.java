package ch.secona.notes2google;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import com.google.api.services.people.v1.model.Url;
import com.google.common.base.Strings;

public class PutDataIntoGoogle {
	private static final String PATH = "out/lotus-notes-data.json";
	private static final String APPLICATION_NAME = "Google People API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";
	private static final String PEOPLE_ME = "people/me";
	private static final String PERSON_ATTRIBUTES = "addresses," + //
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
			"urls,";
	// Global instance of the scopes required by this quick start. If modifying
	// these scopes, delete your previously saved tokens / folder.
	private static final List<String> SCOPES = Arrays.asList(PeopleServiceScopes.CONTACTS);
	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
	private static final String LOTUS_CONTACT_UNID = "LotusContactUnid";
	private static final String LOTUS_CONTACT_CATEGORIES = "LotusContactCategories";
	private static final String LOTUS_CONTACT_MODIFIED = "LotusContactModified";
	private static PeopleService peopleService;

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
		final InputStream in = PutDataIntoGoogle.class.getResourceAsStream(CREDENTIALS_FILE_PATH);

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

		displayLine();
		displayText(APPLICATION_NAME);
		displayLine();
		displayText("");
		displayText("Reading all contacts from Google ...");

		final List<Person> allPersons = getAllPersons(getPeopleService());

		displayText("Number of contacts from Google: " + allPersons.size());
		displayText("Reading all people from Notes (interface file) ...");

		final List<InterfacePerson> interfacePersonList = ReadWriteInterfacePerson.readJson(PATH,
				InterfacePerson[].class);

		displayText("Number of people from Notes (interface file): " + interfacePersonList.size());
		displayText("");
		displayLine();
		displayText("");
		displayText("Contacts in Google but not found in Notes:");
		showPersonsNotInNotes(allPersons, interfacePersonList);
		displayText("");
		displayLine();
		displayText("");
		displayText("People in Notes but not found (or have to be updated) in Google:");
		showContactsNotInGoogleAndAddToGoogle(interfacePersonList, allPersons);
		displayText("");
		displayLine();
		displayText("");
		displayText("NotesId is null in Google:");
		showNotesIdInGoogleIsNull(allPersons);
		displayText("");
		displayLine();
		displayText("");
		displayText("Show all contacts from Google:");

		final List<Person> connections = allPersons;
		int numberOfPersons;

		if (connections != null && connections.size() > 0) {
			numberOfPersons = 0;
			for (final Person person : connections) {
				numberOfPersons++;
				showData(numberOfPersons, person);
			}
		} else {
			displayText("No connections found.");
		}
	}

	private static void showNotesIdInGoogleIsNull(final List<Person> allPersons) {
		allPersons.stream().forEach(p -> {
			final String lotusContactUnid = getLotusContactUnid(p);

			if (Strings.isNullOrEmpty(lotusContactUnid)) {
				System.err.println("NotesId is null: " + p.getNames());
			}
		});
	}

	private static void showContactsNotInGoogleAndAddToGoogle( //
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
			for (final Person googlePerson : allGooglePersons) {
				if (getLotusContactUnid(googlePerson).equalsIgnoreCase(notesId)) {
					foundGooglePerson = googlePerson;
					lotusContactModified = getLotusContactModified(googlePerson);
				}
			}
			if (foundGooglePerson != null) {
				// update
				boolean doUpdate = false;
				final String modified = interfacePerson.getModified();

				if (!Strings.isNullOrEmpty(modified)) {
					if (Strings.isNullOrEmpty(lotusContactModified)) {
						// only modified, so update it
						// ?????? doUpdate = true;
						// final int stopHere = 0;

					} else {
						// both date available, so compare
						final SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");

						try {
							final Date modifiedDate = format.parse(modified);
							final Date lotusContactModifiedDate = format.parse(lotusContactModified);

							if (!modifiedDate.before(lotusContactModifiedDate)) {
								doUpdate = true;
							}
						} catch (final ParseException e) {
							e.printStackTrace();
						}
					}
				}
				if (doUpdate) {
					doUpdateCounter++;

					final String newer = String.format("--> %4d.: ", doUpdateCounter);
					final String resourceName = foundGooglePerson.getResourceName();
					final String resourceNameFormatted = String.format("%-30s", resourceName);

					displayText(resourceNameFormatted + " / " + newer + interfacePerson.toString());
					// GO ON...
//					try {
//						final Person createdContact = getPeopleService().people().updateContact(resourceName, null).execute();
//
//					} catch (IOException | GeneralSecurityException e) {
//						e.printStackTrace();
//					}
				}
			} else {
				notFoundInGoogleCounter++;
				addNotesToGoogle(interfacePerson, notFoundInGoogleCounter);
			}
		}
	}

	private static void addNotesToGoogle(final InterfacePerson p, final int count) {
		final Person contactToCreate = new Person();

		final String id = p.getUid();
		final String firstName = p.getFirstName();
		final String middleName = p.getMiddleName();
		final String lastName = p.getLastName();
		final String phoneBusiness = p.getPhoneBusiness();
		final String phoneBusinessDirect = p.getPhoneBusinessDirect();
		final String phoneBusiness2 = p.getPhoneBusiness2();
		final String mobileBusiness = p.getMobileBusiness();
		final String mobileBusiness2 = p.getMobileBusiness2();
		final String mobilePrivate = p.getMobilePrivate();
		final String mobilePrivate2 = p.getMobilePrivate2();
		final String phonePrivate = p.getPhonePrivate();
		final String faxPrivate = p.getFaxPrivate();
		final String faxBusiness = p.getFaxBusiness();
		// final String emergencyNumber = p.getEmergencyNumber();
		final String company = p.getCompanyName();
		final String jobTitle = p.getJobTitle();
		final String eMailBusiness = p.geteMailBusiness();
		final String eMailPrivate = p.geteMailPrivate();
		final String modified = p.getModified();
		final String categories = p.getCategories();
		final String comment = p.getComment();
		final String birthDate = p.getBirthDate();
		final List<Name> names = new ArrayList<>();
		final String webSite = p.getWebSite();
		// addresses
		final String streetAddress = p.getStreetAddress();
		final String zip = p.getZip();
		final String city = p.getCity();
		final String state = p.getState();
		final String country = p.getCountry();
		final String officeStreetAddress = p.getOfficeStreetAddress();
		final String officeZip = p.getOfficeZip();
		final String officeCity = p.getOfficeCity();
		final String officeState = p.getOfficeState();
		final String officeCountry = p.getOfficeCountry();

		if ("Administrator".equals(lastName)) {
			// don't add the administrator to Google
			return;
		}

		names.add(new Name().setGivenName(firstName).setFamilyName(lastName).setMiddleName(middleName));
		contactToCreate.setNames(names);

		final List<PhoneNumber> phoneNumbers = new ArrayList<>();

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
		phoneNumbers.add(new PhoneNumber().setType("home").setValue(phonePrivate));
		phoneNumbers.add(new PhoneNumber().setType("work").setValue(phoneBusiness));
		phoneNumbers.add(new PhoneNumber().setType("mobile").setValue(mobilePrivate));
		phoneNumbers.add(new PhoneNumber().setType("homeFax").setValue(faxPrivate));
		phoneNumbers.add(new PhoneNumber().setType("workFax").setValue(faxBusiness));
		phoneNumbers.add(new PhoneNumber().setType("otherFax").setValue(""));
		phoneNumbers.add(new PhoneNumber().setType("pager").setValue(""));
		phoneNumbers.add(new PhoneNumber().setType("workMobile").setValue(mobileBusiness));
		phoneNumbers.add(new PhoneNumber().setType("workPager").setValue(""));
		phoneNumbers.add(new PhoneNumber().setType("main").setValue(""));
		phoneNumbers.add(new PhoneNumber().setType("googleVoice").setValue(""));
		phoneNumbers.add(new PhoneNumber().setType("other").setValue(""));
		// others
		phoneNumbers.add(new PhoneNumber().setType("work2").setValue(phoneBusiness2));
		phoneNumbers.add(new PhoneNumber().setType("workDirect").setValue(phoneBusinessDirect));
		phoneNumbers.add(new PhoneNumber().setType("workMobile2").setValue(mobileBusiness2));
		phoneNumbers.add(new PhoneNumber().setType("mobile2").setValue(mobilePrivate2));
		contactToCreate.setPhoneNumbers(phoneNumbers);

		final List<EmailAddress> emailAddresseList = new ArrayList<>();

		// predefined values:
		// 'home'
		// 'work'
		// 'other'
		// or null
		emailAddresseList.add(new EmailAddress().setType("home").setValue(eMailPrivate));
		emailAddresseList.add(new EmailAddress().setType("work").setValue(eMailBusiness));
		contactToCreate.setEmailAddresses(emailAddresseList);

		final List<Address> addressList = new ArrayList<>();
		Address privateAddress = new Address();
		Address officeAddress = new Address();

		privateAddress.setType("home");
		privateAddress.setStreetAddress(streetAddress);
		privateAddress.setPostalCode(zip);
		privateAddress.setCity(city);
		privateAddress.setRegion(state);
		privateAddress.setCountry(country);
		officeAddress.setType("work");
		officeAddress.setStreetAddress(officeStreetAddress);
		officeAddress.setPostalCode(officeZip);
		officeAddress.setCity(officeZip);
		officeAddress.setRegion(officeState);
		officeAddress.setCountry(officeCountry);
		addressList.add(privateAddress);
		addressList.add(officeAddress);
		contactToCreate.setAddresses(addressList);

		final List<Url> urlList = new ArrayList<>();
		final Url officeUrl = new Url();

		officeUrl.setType("work");
		officeUrl.setValue(webSite);
		urlList.add(officeUrl);
		contactToCreate.setUrls(urlList);

		final List<Birthday> birthdayList = new ArrayList<>();

		birthdayList.add(new Birthday().setText(birthDate));
		contactToCreate.setBirthdays(birthdayList);

		final List<Organization> organizationList = new ArrayList<>();

		// predefined values:
		// 'work'
		// 'school'
		// or null
		organizationList.add(new Organization().setType("work").setName(company).setTitle(jobTitle));
		contactToCreate.setOrganizations(organizationList);

		// represents the attribute "notes" in Google
		final List<Biography> biographyList = new ArrayList<>();

		biographyList.add(new Biography().setValue(comment));
		contactToCreate.setBiographies(biographyList);

		// represents "own" data, specially the Notes ID
		// for ensure correct synchronization
		final List<ClientData> clientDataList = new ArrayList<>();

		clientDataList.add(new ClientData().setKey(LOTUS_CONTACT_UNID).setValue(id));
		clientDataList.add(new ClientData().setKey(LOTUS_CONTACT_CATEGORIES).setValue(categories));
		clientDataList.add(new ClientData().setKey(LOTUS_CONTACT_MODIFIED).setValue(modified));
		contactToCreate.setClientData(clientDataList);

		System.out.println(String.format("----> Add Notes person %s to Google.", p));
		System.err.println(String.format("%4d. Not found in Google by NotesId: %s", count,
				p.getFirstName() + " " + p.getLastName()));
		// ---------------------------------------------------------------
		// activate following lines for adding contact/person
		try {
			final Person createdContact = getPeopleService().people().createContact(contactToCreate).execute();

			createdContact.getResourceName();
		} catch (final IOException | GeneralSecurityException e) {
			e.printStackTrace();
		}
		// ---------------------------------------------------------------
	}

	private static void showPersonsNotInNotes(final List<Person> allPersons,
			final List<InterfacePerson> interfacePersonList) {
		int notFoundInNotesCounter = 0;

		checkDoubleNotesIds(allPersons);
		for (final Person p : allPersons) {
			boolean found = false;
			final String lotusContactUnid = getLotusContactUnid(p);

			if (StringUtils.isEmpty(lotusContactUnid)) {
				System.err.println("Lotus Contact Unid is empty: " + p);
			}
			for (final InterfacePerson ip : interfacePersonList) {

				if (ip.getUid().equalsIgnoreCase(lotusContactUnid)) {
					found = true;
				}
			}
			if (!found) {
				notFoundInNotesCounter++;
				System.err.println(String.format("%4d. Not found in Notes: %s", //
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
				System.err.println("Notes Id already found: " + lotusContactUnid + //
						" / " + p);
			}
		}
	}

	private static void showData(final int count, final Person person) {
		final List<Name> names = person.getNames();
		final String lotusContactUnid = getLotusContactUnid(person);
		final String email = getEmail(person);

		if (names != null && names.size() > 0) {
			System.out.println(String.format("%4d. [%s", count, names.get(0).getDisplayName() + //
					", notesId: " + lotusContactUnid + //
					", mail: ]" + email));
		} else {
			System.out.println(String.format("%4d. --> [%s]", count, person));
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
					System.err.println("Type of mail adress is null: " + value);
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
		ListConnectionsResponse fullSyncResponse = peopleService.people().connections().list(PEOPLE_ME)
				.setPersonFields(PERSON_ATTRIBUTES).setRequestSyncToken(true).execute();

		allPersons.addAll(fullSyncResponse.getConnections());
		// fetch all the pages
		while (fullSyncResponse.getNextPageToken() != null) {
			fullSyncResponse = peopleService.people().connections().list(PEOPLE_ME).setPersonFields(PERSON_ATTRIBUTES)
					.setRequestSyncToken(true).setPageToken(fullSyncResponse.getNextPageToken()).execute();
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
}