import javax.swing.JFrame;

public class ClientViewer{

    public static void main(String[] args){
        JFrame frame = new ClientFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("P2P Client");
        frame.setVisible(true);
    }
}
