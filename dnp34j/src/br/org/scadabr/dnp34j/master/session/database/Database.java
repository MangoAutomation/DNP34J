package br.org.scadabr.dnp34j.master.session.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import br.org.scadabr.dnp34j.master.common.DataMapFeatures;

public class Database implements DataMapFeatures {
	private HashMap<Integer, DataBuffer> binaryInputPoints;
	private HashMap<Integer, DataBuffer> binaryOutputPoints;
	private HashMap<Integer, DataBuffer> counterInputPoints;
	private HashMap<Integer, DataBuffer> analogInputPoints;
	private HashMap<Integer, DataBuffer> analogOutputPoints;

	public Database() {
		binaryInputPoints = new HashMap<Integer, DataBuffer>();
		binaryOutputPoints = new HashMap<Integer, DataBuffer>();
		counterInputPoints = new HashMap<Integer, DataBuffer>();
		analogInputPoints = new HashMap<Integer, DataBuffer>();
		analogOutputPoints = new HashMap<Integer, DataBuffer>();
	}

	public List<DataElement> readBinaryInputPoint(int index) {
		if (binaryInputPoints.get(index) != null)
			return binaryInputPoints.get(index).readAndPop();
		else
			return null;
	}

	public DataElement readAnalogInputPoint(int index) {
		if (analogInputPoints.get(index) != null)
			return analogInputPoints.get(index).readLastRecord();
		else
			return null;
	}

	public void writeRecord(DataElement element) {
		getDataBuffer(element.getIndex(), element.getGroup()).insert(element);
	}

	public List<DataElement> read(int index, int group) {
		switch (group) {
		case 0x00:
		case 0x01:
			if (binaryInputPoints.get(index) == null)
				binaryInputPoints.put(index, new DataBuffer());
			return binaryInputPoints.get(index).readAndPop();
		case 0x10:
			if (binaryOutputPoints.get(index) == null)
				binaryOutputPoints.put(index, new DataBuffer());
			return binaryOutputPoints.get(index).readAndPop();
		case 0x20:
			if (counterInputPoints.get(index) == null)
				counterInputPoints.put(index, new DataBuffer());
			return counterInputPoints.get(index).readAndPop();
		case 0x30:
			if (analogInputPoints.get(index) == null)
				analogInputPoints.put(index, new DataBuffer());
			return analogInputPoints.get(index).readAndPop();
		case 0x40:
			if (analogOutputPoints.get(index) == null)
				analogOutputPoints.put(index, new DataBuffer());
			return analogOutputPoints.get(index).readAndPop();
		default:
			return new ArrayList<DataElement>();
		}
	}

	private DataBuffer getDataBuffer(int index, int group) {
		switch (group) {
		case 0x00:
		case 0x01:
			if (binaryInputPoints.get(index) == null)
				binaryInputPoints.put(index, new DataBuffer());
			return binaryInputPoints.get(index);
		case 0x10:
			if (binaryOutputPoints.get(index) == null)
				binaryOutputPoints.put(index, new DataBuffer());
			return binaryOutputPoints.get(index);
		case 0x20:
			if (counterInputPoints.get(index) == null)
				counterInputPoints.put(index, new DataBuffer());
			return counterInputPoints.get(index);
		case 0x30:
			if (analogInputPoints.get(index) == null)
				analogInputPoints.put(index, new DataBuffer());
			return analogInputPoints.get(index);
		case 0x40:
			if (analogOutputPoints.get(index) == null)
				analogOutputPoints.put(index, new DataBuffer());
			return analogOutputPoints.get(index);
		default:
			return new DataBuffer();
		}
	}

	public HashMap<Integer, DataBuffer> getBinaryInputPoints() {
		return binaryInputPoints;
	}

	public void setBinaryInputPoints(
			HashMap<Integer, DataBuffer> binaryInputPoints) {
		this.binaryInputPoints = binaryInputPoints;
	}

	public HashMap<Integer, DataBuffer> getBinaryOutputPoints() {
		return binaryOutputPoints;
	}

	public void setBinaryOutputPoints(
			HashMap<Integer, DataBuffer> binaryOutputPoints) {
		this.binaryOutputPoints = binaryOutputPoints;
	}

	public HashMap<Integer, DataBuffer> getCounterInputPoints() {
		return counterInputPoints;
	}

	public void setCounterInputPoints(
			HashMap<Integer, DataBuffer> counterInputPoints) {
		this.counterInputPoints = counterInputPoints;
	}

	public HashMap<Integer, DataBuffer> getAnalogInputPoints() {
		return analogInputPoints;
	}

	public void setAnalogInputPoints(
			HashMap<Integer, DataBuffer> analogInputPoints) {
		this.analogInputPoints = analogInputPoints;
	}

	public HashMap<Integer, DataBuffer> getAnalogOutputPoints() {
		return analogOutputPoints;
	}

	public void setAnalogOutputPoints(
			HashMap<Integer, DataBuffer> analogOutputPoints) {
		this.analogOutputPoints = analogOutputPoints;
	}

}
