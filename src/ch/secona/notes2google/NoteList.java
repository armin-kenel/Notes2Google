package ch.secona.notes2google;

import java.util.List;

public class NoteList {
	private String name;
	private List<Object> objectList;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Object> getObjectList() {
		return objectList;
	}

	public void setObjectList(List<Object> objectList) {
		this.objectList = objectList;
	}

	@Override
	public String toString() {
		return "NoteList [name=" + name + ", objectList=" + objectList + "]";
	}
}