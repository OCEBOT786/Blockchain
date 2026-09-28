// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract SupplyChainOrder {
    enum Status {
        OrderReceived,
        InTransit,
        Delivered
    }

    struct Order {
        uint256 orderId;
        address customerAddress;
        Status status;
        uint256 timestamp;
    }

    address public owner;
    uint256 public orderCount;

    mapping(uint256 => Order) public orders;
    mapping(address => bool) public authorizedOperators;

    event OrderCreated(uint256 indexed orderId, address indexed customerAddress, uint256 timestamp);
    event OrderStatusUpdated(uint256 indexed orderId, Status newStatus, uint256 timestamp);
    event OperatorAuthorized(address indexed operator);

    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this");
        _;
    }

    modifier onlyOperator() {
        require(authorizedOperators[msg.sender], "Not an authorized operator");
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    function authorizeOperator(address _operator) external onlyOwner {
        require(_operator != address(0), "Invalid operator address");
        authorizedOperators[_operator] = true;
        emit OperatorAuthorized(_operator);
    }

    function createOrder(address _customer) external onlyOwner returns (uint256) {
        require(_customer != address(0), "Invalid customer address");

        orderCount++;
        orders[orderCount] = Order({
            orderId: orderCount,
            customerAddress: _customer,
            status: Status.OrderReceived,
            timestamp: block.timestamp
        });

        emit OrderCreated(orderCount, _customer, block.timestamp);
        return orderCount;
    }

    function updateOrderStatus(uint256 _orderId, Status _newStatus) external onlyOperator {
        require(_orderId > 0 && _orderId <= orderCount, "Order does not exist");

        orders[_orderId].status = _newStatus;
        orders[_orderId].timestamp = block.timestamp;

        emit OrderStatusUpdated(_orderId, _newStatus, block.timestamp);
    }

    function getOrder(uint256 _orderId) external view returns (
        uint256 orderId,
        address customerAddress,
        Status status,
        uint256 timestamp
    ) {
        require(_orderId > 0 && _orderId <= orderCount, "Order does not exist");
        Order memory order = orders[_orderId];
        return (order.orderId, order.customerAddress, order.status, order.timestamp);
    }
}
