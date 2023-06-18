package rmiModel;

import java.util.Vector;

public class Files {

    Vector<String> files;

    public Files(Vector<String> files) {
        this.files = files;
    }

    public boolean contains(String fileName) {
        return files.contains(fileName);
    }
}
