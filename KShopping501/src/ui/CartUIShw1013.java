package ui;




import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import dao.ShippingDAO;
import dao.CartDAOShw1013;
import dto.CartDTOShw1013;
import dto.UserDTO;
import simulate.Carrier;

public class CartUIShw1013 extends JFrame {

    private final CartDAOShw1013 cartDAOShw1013; // 데이터 액세스 객체
    private final UserDTO currentUser; // 현재 사용자 정보
    private List<CartDTOShw1013> products; // 장바구니 상품 리스트
    private final JLabel totalLabel; // 총 금액 표시 라벨
    private int total = 0; // 총 금액 변수

    public CartUIShw1013(UserDTO user) {
        this.currentUser = user; 
        this.cartDAOShw1013 = new CartDAOShw1013();

        // 창 설정
        setTitle(currentUser.getUsername() + "님의 장바구니");
        setSize(800, 600);
        setLocationRelativeTo(null);


        // 총 금액 라벨 초기화
        totalLabel = new JLabel("총 구매 금액: 0원", SwingConstants.CENTER);

        // 테이블 설정
        String[] columnNames = {"상품명", "가격", "개수", "재고", "삭제", "수량 수정"};
        NonEditableTableModel model = new NonEditableTableModel(columnNames, 0);
        loadTableData(model);

        JTable table = new JTable(model);
        configureTable(table, model);

        JScrollPane scrollPane = new JScrollPane(table);

        // 결제 버튼
        JButton checkoutButton = new JButton("결제하기");
        checkoutButton.addActionListener(e -> processCheckout(model));

        // 하단 패널 설정
        JPanel bottomPanel = createBottomPanel(checkoutButton);

        // 컴포넌트 배치
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 하단 패널을 생성하는 메서드
     */
    private JPanel createBottomPanel(JButton checkoutButton) {
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // 총 금액 라벨 추가
        gbc.gridx = 0;
        gbc.gridy = 0;
        bottomPanel.add(totalLabel, gbc);

        // 결제 버튼 추가
        gbc.gridy = 1;
        bottomPanel.add(checkoutButton, gbc);

        return bottomPanel;
    }

    /**
     * 테이블 데이터를 로드하는 메서드
     */
    private void loadTableData(DefaultTableModel model) {
        // products = cartDAOShw1013.getAllCartItems();
        products = cartDAOShw1013.getCartItems(currentUser.getUserId());
        model.setRowCount(0); // 기존 데이터 초기화
        for (CartDTOShw1013 product : products) {
            model.addRow(new Object[]{
                    product.getProductName(),
                    product.getPrice(),
                    product.getQuantity(),
                    product.getStock(),
                    "삭제",
                    "수량 수정"
            });
        }
        updateTotalPrice(model);
    }

    /**
     * 테이블을 구성하는 메서드
     */
    private void configureTable(JTable table, DefaultTableModel model) {
        // 테이블 셀 렌더러 설정
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // 상품명
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // 개수
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // 재고
        table.getColumnModel().getColumn(1).setCellRenderer(new PriceCellRenderer()); // 가격

        // 버튼 렌더러 및 에디터 설정
        table.getColumn("삭제").setCellRenderer(new ButtonRenderer());
        table.getColumn("삭제").setCellEditor(new ButtonEditor(new JCheckBox(), model, "삭제"));

        table.getColumn("수량 수정").setCellRenderer(new ButtonRenderer());
        table.getColumn("수량 수정").setCellEditor(new ButtonEditor(new JCheckBox(), model, "수량 수정"));

        table.setRowHeight((int) (table.getRowHeight() * 1.5));
    }

    /**
     * 결제 처리를 수행하는 메서드
     */
    private void processCheckout(DefaultTableModel model) {
        int confirmation = JOptionPane.showConfirmDialog(this, "결제를 진행하시겠습니까?", "결제 확인", JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_OPTION) {
            // boolean success = cartDAOShw1013.order(currentUser.getUserId(), total);
            Integer orderId = cartDAOShw1013.order(currentUser.getUserId(), total);
            if (orderId != null) {
                JOptionPane.showMessageDialog(this, "결제가 완료되었습니다.");
                model.setRowCount(0); // 테이블 초기화
                loadTableData(model);
              
                ShippingDAO shippingDAO = new ShippingDAO();
                shippingDAO.addShipping(orderId.intValue(), UUID.randomUUID().toString(), Carrier.getCarrier(), "배송중");
            } else {
                JOptionPane.showMessageDialog(this, "결제 처리 중 오류가 발생했습니다.");

            }
        }
    }

    /**
     * 총 금액을 업데이트하는 메서드
     */
    private void updateTotalPrice(DefaultTableModel model) {
        total = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            int price = Integer.parseInt(model.getValueAt(i, 1).toString());
            int quantity = Integer.parseInt(model.getValueAt(i, 2).toString());
            total += price * quantity;
        }
        totalLabel.setText("총 구매 금액: " + NumberFormat.getNumberInstance(Locale.KOREA).format(total) + " 원");
    }

    // 버튼 렌더러
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            return this;
        }
    }

    // 버튼 에디터
    class ButtonEditor extends DefaultCellEditor {
        private final DefaultTableModel model;
        private final String actionType;
        private final JButton button;
        private int row;

        public ButtonEditor(JCheckBox checkBox, DefaultTableModel model, String actionType) {
            super(checkBox);
            this.model = model;
            this.actionType = actionType;
            this.button = new JButton(actionType);
            button.setOpaque(true);
            button.addActionListener(e -> SwingUtilities.invokeLater(() -> handleButtonAction()));
        }

        private void handleButtonAction() {
            JTable table = (JTable) SwingUtilities.getAncestorOfClass(JTable.class, button);

            // 편집 중이면 강제로 종료
            if (table != null && table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }

            if (row >= 0 && row < products.size()) {
                CartDTOShw1013 product = products.get(row);

                if ("삭제".equals(actionType)) {
                    // 데이터베이스에서 삭제
                    cartDAOShw1013.deleteCartItem(product.getCartId());

                    // 데이터 리스트와 테이블 모델에서 삭제
                    products.remove(row);
                    model.removeRow(row);

                    // 삭제 후 총 금액 업데이트
                    SwingUtilities.invokeLater(() -> {
                        updateTotalPrice(model);

                        // 테이블이 비어있지 않을 경우 포커스 재설정
                        if (model.getRowCount() > 0) {
                            int nextRow = Math.min(row, model.getRowCount() - 1);
                            table.setRowSelectionInterval(nextRow, nextRow);
                        }
                    });
                } else if ("수량 수정".equals(actionType)) {
                    String newQuantityStr = JOptionPane.showInputDialog("새로운 수량을 입력하세요:", product.getQuantity());
                    try {
                        int newQuantity = Integer.parseInt(newQuantityStr);
                        if (newQuantity > 0 && newQuantity <= product.getStock()) {
                            cartDAOShw1013.updateCartItem(product.getCartId(), newQuantity);
                            product.setQuantity(newQuantity);
                            model.setValueAt(newQuantity, row, 2);

                            SwingUtilities.invokeLater(() -> updateTotalPrice(model));
                        } else {
                            JOptionPane.showMessageDialog(null, "유효한 수량을 입력하세요.");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "숫자를 입력하세요.");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "유효하지 않은 행입니다. 다시 시도하세요.");
            }
        }




        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            button.setText(actionType);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return actionType;
        }
    }

    // 가격 렌더러
    class PriceCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            if (value != null) {
                setText(NumberFormat.getNumberInstance(Locale.KOREA).format(value) + " 원");
            }
            return c;
        }
    }

    // 수정 불가능한 테이블 모델
    class NonEditableTableModel extends DefaultTableModel {
        public NonEditableTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 4 || column == 5; // 버튼 열만 편집 가능
        }
    }
}