package br.org.scadabr.dnp34j.master.session.database;

import java.util.ArrayList;
import java.util.List;

public class DataBuffer {
    public static final int MAX_SIZE = 512;
    public static final int DEFAULT_SIZE = 128;
    public static final int MIN_SIZE = 1;

    private DataElement[] data;
    private int index;

    public DataBuffer() {
        this(DEFAULT_SIZE);
    }

    public DataBuffer(int size) {
        if ((size < MIN_SIZE) || (size > MAX_SIZE)) {
            throw new IllegalArgumentException();
        }
        index = 0;
        data = new DataElement[size];
    }

    public DataElement readLastRecord() {
        if (index > 0)
            return data[index - 1];
        else
            return data[data.length - 1];
    }

    public List<DataElement> readAndPop() {
        List<DataElement> lista = new ArrayList<DataElement>();
        for (int i = 0; i < index; i++) {
            lista.add(data[i]);
        }

        index = 0;

        return lista;
    }

    public int insert(DataElement element) {
        data[index] = element;
        incrementIndex();
        return (index - 1);
    }

    public int remove() {
        decrementIndex();
        return (index);
    }

    private void incrementIndex() {
        if (data.length - 1 == index)
            index = 0;
        else
            index++;
    }

    private void decrementIndex() {
        if (index == 0)
            index = data.length - 1;
        else
            index--;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public DataElement[] getData() {
        return data;
    }

    public void setData(DataElement[] data) {
        this.data = data;
    }

}
