import javax.swing.*;
import java.util.LinkedList;

/**
 * Created by Julian M. <julian@ddspn.org> on 21.02.14.
 */
public class Ddsn {

    private static LinkedList<Peer> peers = new LinkedList<Peer>();
    public static MainForm mainForm;

    public static void main(String[] args) {
        mainForm = new MainForm();

        JFrame frame = new JFrame("MainForm");
        frame.setContentPane(mainForm.getMainPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        addPeer(new Peer(new Code()));
    }

    public static void addPeer(Peer peer) {
        peers.add(peer);
        mainForm.addPeer(peer);
    }

}
