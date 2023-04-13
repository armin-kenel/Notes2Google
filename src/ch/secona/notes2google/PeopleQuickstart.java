package ch.secona.notes2google;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

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
import com.google.api.services.people.v1.PeopleService.People;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.people.v1.model.ExternalId;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;

public class PeopleQuickstart {
	private static final String APPLICATION_NAME = "Google People API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = Arrays.asList(PeopleServiceScopes.CONTACTS_READONLY);
	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

	/**
	 * Creates an authorized Credential object.
	 *
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		final InputStream in = PeopleQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);

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

	public static void main(final String... args) throws IOException, GeneralSecurityException {
		// Build a new authorized API client service.
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		final PeopleService service = new PeopleService.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				getCredentials(HTTP_TRANSPORT)).setApplicationName(APPLICATION_NAME).build();
		final People people = service.people();
		// Request 10 connections.
		final ListConnectionsResponse response = people.connections(). //
				list("people/me").setPageSize(100).setPersonFields("addresses," + //
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
						"urls,")
				.execute();
		// Print display name of connections if available.
		final List<Person> connections = response.getConnections();

		if (connections != null && connections.size() > 0) {
			int count = 0;
			System.out.println("size: " + connections.size());
			for (final Person person : connections) {
				final List<Name> names = person.getNames();
				final List<ExternalId> externalIds = person.getExternalIds();

				if (names != null && names.size() > 0) {
					count++;
					final List<PhoneNumber> listOfPhoneNumbers = person.getPhoneNumbers();
					final Name name = person.getNames().get(0);

					System.out.println(count);
					System.out.println("Name   : " + name.toPrettyString());
					System.out.println("Name   : " + name.toString());
					System.out.println("Name   : " + name.getDisplayName());
					System.out.println("phone  : " + listOfPhoneNumbers);
					System.out.println("ext Ids: " + externalIds);
					System.out.println("--------------------------------------");
				} else {
					System.out.println("No names available for connection.");
				}
			}
		} else {
			System.out.println("No connections found.");
		}
	}
}