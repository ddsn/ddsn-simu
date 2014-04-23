import java.util.Vector;

/**
 * Created by Julian M. <julian@ddspn.org> on 21.02.14.
 */
public class Code {

    private Vector<Integer> layers;
    private int size;

    public Code() {
        layers = new Vector<Integer>(2);
        size = 0;
    }

    public Code(String code) {
        layers = new Vector<Integer>(2);

        for (int i = 0; i < code.length(); i++) {
            setLayerCode(i, code.charAt(i) == '0' ? 0 : 1);
        }
    }

    public boolean contains(Code code) {
        for (int i = 0; i < size; i++) {
            if (getLayerCode(i) != code.getExtLayerCode(i)) {
                return false;
            }
        }
        return true;
    }

    public int getDifferingLayer(Code code) {
        for (int i = 0; i < layers.size(); i++) {
            if (getLayerCode(i) != code.getExtLayerCode(i)) {
                return i;
            }
        }
        return -1;
    }

    public int getLayerCode(int layer) {
        return layers.get(layer);
    }

    public int getExtLayerCode(int layer) {
        if (layer >= layers.size()) return 0;
        return layers.get(layer);
    }

    public void setLayerCode(int layer, int code) {
        if (layers.size() == 0) {
            layers.setSize(2);
        }

        while (layer >= layers.size()) {
            layers.setSize(layers.size() * 2);
        }

        layers.set(layer, code);

        if (layer >= size) {
            size = layer + 1;
        }
    }

    public int getLayers() {
        return size;
    }

    @Override
    public String toString() {
        if (size == 0) return "X";
        StringBuffer codeString = new StringBuffer();
        for (int i = 0; i < size; i++) {
            codeString.append(layers.get(i) == 0 ? '0' : '1');
        }
        return codeString.toString();
    }

    @Override
    public Code clone() {
        Code code = new Code();
        code.layers = (Vector<Integer>) layers.clone();
        code.size = size;
        return code;
    }

}
