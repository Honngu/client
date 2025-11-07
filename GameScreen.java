package Client;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class GameScreen extends JFrame {
    private String username;
    private NetworkHandler network;
    private boolean isHost;
    private String roomName;

    private JLabel[] cardLabels = new JLabel[3];
    private JButton btnStart;
    private JButton btnDraw;
    private boolean canDraw = false;
    private int cardsDrawn = 0; // Đếm số bài đã rút
    int[] a = new int[3];
    private List<Integer> list = new ArrayList<>();

    public GameScreen(String username, NetworkHandler network, boolean isHost, String roomName) {
        this.username = username;
        this.network = network;
        this.isHost = isHost;
        this.roomName = roomName;

        setTitle("Phòng " + roomName + " - " + username + (isHost ? " (Chủ phòng)" : ""));
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ===== Bàn chơi =====
        JPanel gamePanel = new JPanel(new GridLayout(1, 3, 10, 10));
        for (int i = 0; i < 3; i++) {
            cardLabels[i] = new JLabel("[Chưa rút]", SwingConstants.CENTER);
            cardLabels[i].setOpaque(true);
            cardLabels[i].setBackground(Color.LIGHT_GRAY);
            gamePanel.add(cardLabels[i]);
        }

        // ===== Nút điều khiển =====
        JPanel bottomPanel = new JPanel();
        btnStart = new JButton("Bắt đầu");
        btnDraw = new JButton("Rút bài");

        btnStart.setEnabled(isHost); // Chỉ host mới có thể bắt đầu
        btnDraw.setEnabled(false); // Chưa thể rút cho đến khi game Ready

        bottomPanel.add(btnStart);
        bottomPanel.add(btnDraw);

        add(gamePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // ===== Lắng nghe server =====
        network.startListening(this::handleGameMessage);

        // ===== Sự kiện nút =====
        btnStart.addActionListener(e -> {
            try {
                network.sendMsg("START;" + roomName);
                btnStart.setEnabled(false);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "❌ Lỗi gửi lệnh bắt đầu.");
            }
        });

        btnDraw.addActionListener(e -> {
            if (canDraw && cardsDrawn < 3) {
                try {
                    // Cần xác định Player ID cụ thể, ở đây dùng ID mặc định là 0 (hoặc Player ID
                    // do Server gán)
                    network.sendMsg("0:Draw");
                    btnDraw.setEnabled(false); // Vô hiệu hóa sau khi gửi yêu cầu rút
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "❌ Lỗi gửi yêu cầu rút bài.");
                }
            }
        });
        setVisible(true);
    }

    private void handleGameMessage(String msg) {
        System.out.println("🎮 [Game] Nhận: " + msg);

        if (msg.startsWith("READY")) {
            SwingUtilities.invokeLater(() -> {
                for (JLabel label : cardLabels)
                    label.setText("[Chưa rút]");
                cardsDrawn = 0;
                canDraw = true;
                btnDraw.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Trò chơi bắt đầu! Hãy rút bài.");
            });
        }
        else if (msg.startsWith("DRAW;")) {
            int card = Integer.parseInt(msg.split(";")[1]);
            SwingUtilities.invokeLater(() -> updateCard(card));
            list.add(card);

         }
        else if (msg.startsWith("END")) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "🃏 Kết thúc lượt rút bài!");

                try {
                    network.sendMsg(username + " : " + String.join(" ", list.stream()
                            .map(String::valueOf)
                            .toArray(String[]::new)));

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                canDraw = false;
                btnDraw.setEnabled(false);
            });
        }
    }

    private void updateCard(int value) {
        for (JLabel label : cardLabels) {
            if (label.getText().equals("[Chưa rút]")) {
                label.setText("Bài: " + value);
                cardsDrawn++;
                btnDraw.setEnabled(cardsDrawn < 3); // Cho phép rút tiếp nếu chưa đủ 3 lá
                break;
            }
        }
    }
}