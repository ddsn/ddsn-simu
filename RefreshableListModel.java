import javax.swing.*;
import java.util.Vector;

/**
 * Created by Julian M. <julian@ddsn.org> on 23.02.14.
 */
public class RefreshableListModel<E> extends AbstractListModel<E> {
    private Vector<E> elements = new Vector<E>();

    @Override
    public int getSize() {
        return elements.size();
    }

    @Override
    public E getElementAt(int index) {
        return elements.get(index);
    }

    public void addElement(E element) {
        elements.addElement(element);
        int index = elements.size();
        fireIntervalAdded(this, index, index);
    }

    public void removeElement(E element) {
        int index = elements.indexOf(element);
        elements.remove(index);
        fireIntervalRemoved(this, index, index);
    }

    public void removeElement(int index) {
        fireIntervalRemoved(this, index, index);
    }

    public void refreshElement(E element) {
        int index = elements.indexOf(element);
        fireContentsChanged(this, index, index);
    }

    public void refreshElement(int index) {
        fireContentsChanged(this, index, index);
    }
}
