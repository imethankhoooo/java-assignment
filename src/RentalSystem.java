import java.util.*;
import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 主系统类：管理账户、车辆、租赁和核心操作
 */
public class RentalSystem {
    private List<Account> accounts;
    private List<Vehicle> vehicles;
    private List<Rental> rentals;
    private List<Customer> customers;
    private NotificationService notificationService;
    private TicketService ticketService;
    private int nextRentalId = 1;
    private int nextCustomerId = 1;

    public RentalSystem() {
        accounts = new ArrayList<>();
        vehicles = new ArrayList<>();
        rentals = new ArrayList<>();
        customers = new ArrayList<>();
        notificationService = new NotificationService();
        ticketService = new TicketService();
    }

    /**
     * 从JSON文件加载维护记录数据
     */
    public void loadMaintenanceLogs(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            
            parseMaintenanceLogsFromJson(jsonContent.toString());
        } catch (IOException e) {
            System.out.println("Failed to load maintenance logs: " + e.getMessage());
        }
    }

    /**
     * 保存维护记录数据到JSON文件
     */
    public void saveMaintenanceLogs(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            String jsonContent = convertMaintenanceLogsToJson();
            writer.println(jsonContent);
        } catch (IOException e) {
            System.out.println("Failed to save maintenance logs: " + e.getMessage());
        }
    }

    /**
     * 从JSON文件加载账户数据
     */
    public void loadAccounts(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            
                    accounts = parseAccountsFromJson(jsonContent.toString());
        if (accounts == null) accounts = new ArrayList<>();
        
        // 加载用户邮箱信息到通知服务
        notificationService.loadUserEmailsFromAccounts(accounts);
        } catch (IOException e) {
            System.out.println("Failed to load account data: " + e.getMessage());
            accounts = new ArrayList<>();
        }
    }

    /**
     * 从JSON文件加载车辆数据
     */
    public void loadVehicles(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            
            vehicles = parseVehiclesFromJson(jsonContent.toString());
            if (vehicles == null) vehicles = new ArrayList<>();
        } catch (IOException e) {
            System.out.println("Failed to load vehicle data: " + e.getMessage());
            vehicles = new ArrayList<>();
        }
    }

    /**
     * 从JSON文件加载租赁数据
     */
    public void loadRentals(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            
            rentals = parseRentalsFromJson(jsonContent.toString());
            if (rentals == null) rentals = new ArrayList<>();
            
            // 更新下一个租赁ID
            for (Rental r : rentals) {
                if (r.getId() >= nextRentalId) {
                    nextRentalId = r.getId() + 1;
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to load rental data: " + e.getMessage());
            rentals = new ArrayList<>();
        }
    }

    /**
     * 保存租赁数据到JSON文件
     */
    public void saveRentals(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            String jsonContent = convertRentalsToJson();
            writer.println(jsonContent);
        } catch (IOException e) {
            System.out.println("Failed to save rental data: " + e.getMessage());
        }
    }

    /**
     * 解析账户JSON数据
     */
    private List<Account> parseAccountsFromJson(String json) {
        List<Account> accountList = new ArrayList<>();
        try {
            // 简单的JSON解析，假设格式规范
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1); // 移除外层[]
                
                // 分割各个账户对象
                String[] accountObjects = splitJsonObjects(json);
                
                for (String accountJson : accountObjects) {
                    Account account = parseAccountFromJson(accountJson.trim());
                    if (account != null) {
                        accountList.add(account);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to parse account JSON: " + e.getMessage());
        }
        return accountList;
    }

    /**
     * 解析单个账户对象
     */
    private Account parseAccountFromJson(String json) {
        try {
            String username = extractJsonValue(json, "username");
            String password = extractJsonValue(json, "password");
            String roleStr = extractJsonValue(json, "role");
            String email = extractJsonValue(json, "email");
            String fullName = extractJsonValue(json, "fullName");
            String contactNumber = extractJsonValue(json, "contactNumber");
            
            if (username != null && password != null && roleStr != null) {
                AccountRole role = AccountRole.valueOf(roleStr);
                return new Account(username, password, role, 
                    email != null ? email : "", 
                    fullName != null ? fullName : "", 
                    contactNumber != null ? contactNumber : "");
            }
        } catch (Exception e) {
            System.out.println("Failed to parse account object: " + e.getMessage());
        }
        return null;
    }

    /**
     * 解析车辆JSON数据
     */
    private List<Vehicle> parseVehiclesFromJson(String json) {
        List<Vehicle> vehicleList = new ArrayList<>();
        try {
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
                
                String[] vehicleObjects = splitJsonObjects(json);
                
                for (String vehicleJson : vehicleObjects) {
                    Vehicle vehicle = parseVehicleFromJson(vehicleJson.trim());
                    if (vehicle != null) {
                        vehicleList.add(vehicle);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("解析车辆JSON失败: " + e.getMessage());
        }
        return vehicleList;
    }

    /**
     * 解析单个车辆对象
     */
    private Vehicle parseVehicleFromJson(String json) {
        try {
            int id = Integer.parseInt(extractJsonValue(json, "id"));
            String brand = extractJsonValue(json, "brand");
            String model = extractJsonValue(json, "model");
            String statusStr = extractJsonValue(json, "status");
            double insuranceRate = Double.parseDouble(extractJsonValue(json, "insuranceRate"));
            
            // 解析价格，如果没有则使用默认值
            String priceStr = extractJsonValue(json, "basePrice");
            double basePrice = (priceStr != null) ? Double.parseDouble(priceStr) : 50.0;
            
            // 解析新增的车辆属性
            String carPlate = extractJsonValue(json, "carPlate");
            if (carPlate == null) carPlate = "UNKNOWN";
            
            String vehicleTypeStr = extractJsonValue(json, "vehicleType");
            VehicleType vehicleType = VehicleType.CAR; // 默认值
            if (vehicleTypeStr != null) {
                try {
                    vehicleType = VehicleType.valueOf(vehicleTypeStr);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid vehicle type: " + vehicleTypeStr + ", using default CAR");
                }
            }
            
            String fuelTypeStr = extractJsonValue(json, "fuelType");
            FuelType fuelType = FuelType.PETROL; // 默认值
            if (fuelTypeStr != null) {
                try {
                    fuelType = FuelType.valueOf(fuelTypeStr);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid fuel type: " + fuelTypeStr + ", using default PETROL");
                }
            }
            
            VehicleStatus status = VehicleStatus.valueOf(statusStr);
            
            // 解析长期折扣
            Map<Integer, Double> discounts = new HashMap<>();
            String discountsJson = extractJsonObject(json, "longTermDiscounts");
            if (discountsJson != null && !discountsJson.equals("null")) {
                // 简单解析折扣对象
                discountsJson = discountsJson.replace("{", "").replace("}", "");
                String[] pairs = discountsJson.split(",");
                for (String pair : pairs) {
                    if (pair.contains(":")) {
                        String[] keyValue = pair.split(":");
                        int days = Integer.parseInt(keyValue[0].trim().replace("\"", ""));
                        double discount = Double.parseDouble(keyValue[1].trim());
                        discounts.put(days, discount);
                    }
                }
            }
            
            // 使用新的构造函数创建Vehicle对象
            return new Vehicle(id, brand, model, carPlate, vehicleType, fuelType, status, insuranceRate, basePrice, discounts);
        } catch (Exception e) {
            System.out.println("Failed to parse vehicle object: " + e.getMessage());
        }
        return null;
    }

    /**
     * 解析租赁JSON数据
     */
    private List<Rental> parseRentalsFromJson(String json) {
        List<Rental> rentalList = new ArrayList<>();
        try {
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
                
                String[] rentalObjects = splitJsonObjects(json);
                
                for (String rentalJson : rentalObjects) {
                    Rental rental = parseRentalFromJson(rentalJson.trim());
                    if (rental != null) {
                        rentalList.add(rental);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("解析租赁JSON失败: " + e.getMessage());
        }
        return rentalList;
    }

    /**
     * 解析单个租赁对象
     */
    private Rental parseRentalFromJson(String json) {
        try {
            int id = Integer.parseInt(extractJsonValue(json, "id"));
            double fee = Double.parseDouble(extractJsonValue(json, "fee"));
            String actualFeeStr = extractJsonValue(json, "actualFee");
            double actualFee = (actualFeeStr != null) ? Double.parseDouble(actualFeeStr) : 0.0;
            boolean insurance = Boolean.parseBoolean(extractJsonValue(json, "insurance"));
            String statusStr = extractJsonValue(json, "status");
            String startDateStr = extractJsonValue(json, "startDate");
            String endDateStr = extractJsonValue(json, "endDate");
            
            RentalStatus status = RentalStatus.valueOf(statusStr);
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            
            // 解析客户信息
            String customerJson = extractJsonObject(json, "customer");
            Customer customer = parseCustomerFromJson(customerJson);
            
            // 解析车辆信息
            String vehicleJson = extractJsonObject(json, "vehicle");
            Vehicle vehicle = parseVehicleFromJson(vehicleJson);
            
            // 解析用户名，如果没有则设为null
            String username = extractJsonValue(json, "username");
            
            if (customer != null && vehicle != null) {
                Rental rental = new Rental(id, customer, vehicle, startDate, endDate, status, fee, insurance, username);
                rental.setActualFee(actualFee);
                
                // 解析提醒记录
                String dueSoonReminderStr = extractJsonValue(json, "dueSoonReminderSent");
                if (dueSoonReminderStr != null) {
                    rental.setDueSoonReminderSent(Boolean.parseBoolean(dueSoonReminderStr));
                }
                
                String overdueReminderStr = extractJsonValue(json, "overdueReminderSent");
                if (overdueReminderStr != null) {
                    rental.setOverdueReminderSent(Boolean.parseBoolean(overdueReminderStr));
                }
                
                // 解析票据信息
                String ticketJson = extractJsonObject(json, "ticket");
                if (ticketJson != null && !ticketJson.equals("null")) {
                    loadTicketFromJson(ticketJson, rental);
                }
                
                return rental;
            }
        } catch (Exception e) {
            System.out.println("Failed to parse rental object: " + e.getMessage());
        }
        return null;
    }

    /**
     * 解析客户对象
     */
    private Customer parseCustomerFromJson(String json) {
        try {
            int id = Integer.parseInt(extractJsonValue(json, "id"));
            String name = extractJsonValue(json, "name");
            String contact = extractJsonValue(json, "contact");
            return new Customer(id, name, contact);
        } catch (Exception e) {
            System.out.println("Failed to parse client object: " + e.getMessage());
        }
        return null;
    }

    /**
     * 从JSON加载票据信息到TicketService
     */
    private void loadTicketFromJson(String ticketJson, Rental rental) {
        try {
            String ticketId = extractJsonValue(ticketJson, "ticketId");
            String generatedTimeStr = extractJsonValue(ticketJson, "generatedTime");
            String pickupLocation = extractJsonValue(ticketJson, "pickupLocation");
            String specialInstructions = extractJsonValue(ticketJson, "specialInstructions");
            String isUsedStr = extractJsonValue(ticketJson, "isUsed");
            
            if (ticketId != null) {
                // 创建票据对象
                Ticket ticket = new Ticket(rental);
                
                // 设置票据属性（需要修改Ticket类来支持这些setter）
                ticket.setPickupLocation(pickupLocation != null ? pickupLocation : "Main Office - Vehicle Rental Center");
                ticket.setSpecialInstructions(specialInstructions != null ? specialInstructions : "Please bring valid ID and this ticket for vehicle pickup");
                
                if (isUsedStr != null && Boolean.parseBoolean(isUsedStr)) {
                    ticket.markAsUsed();
                }
                
                // 将票据加载到TicketService中
                ticketService.loadTicket(ticket);
                

            }
        } catch (Exception e) {
            System.out.println("Failure to parse ticket information: " + e.getMessage());
        }
    }

    /**
     * 将租赁数据转换为JSON
     */
    private String convertRentalsToJson() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        
        for (int i = 0; i < rentals.size(); i++) {
            Rental rental = rentals.get(i);
            json.append("  {\n");
            json.append("    \"id\": ").append(rental.getId()).append(",\n");
            json.append("    \"fee\": ").append(rental.getTotalFee()).append(",\n");
            json.append("    \"actualFee\": ").append(rental.getActualFee()).append(",\n");
            json.append("    \"insurance\": ").append(rental.isInsuranceSelected()).append(",\n");
            json.append("    \"status\": \"").append(rental.getStatus()).append("\",\n");
            json.append("    \"startDate\": \"").append(rental.getStartDate()).append("\",\n");
            json.append("    \"endDate\": \"").append(rental.getEndDate()).append("\",\n");
            
            // 客户信息
            Customer customer = rental.getCustomer();
            json.append("    \"customer\": {\n");
            json.append("      \"id\": ").append(customer.getId()).append(",\n");
            json.append("      \"name\": \"").append(customer.getName()).append("\",\n");
            json.append("      \"contact\": \"").append(customer.getContact()).append("\"\n");
            json.append("    },\n");
            
            // 车辆信息
            Vehicle vehicle = rental.getVehicle();
            json.append("    \"vehicle\": {\n");
            json.append("      \"id\": ").append(vehicle.getId()).append(",\n");
            json.append("      \"brand\": \"").append(vehicle.getBrand()).append("\",\n");
            json.append("      \"model\": \"").append(vehicle.getModel()).append("\",\n");
            json.append("      \"carPlate\": \"").append(vehicle.getCarPlate()).append("\",\n");
            json.append("      \"vehicleType\": \"").append(vehicle.getVehicleType()).append("\",\n");
            json.append("      \"fuelType\": \"").append(vehicle.getFuelType()).append("\",\n");
            json.append("      \"status\": \"").append(vehicle.getStatus()).append("\",\n");
            json.append("      \"insuranceRate\": ").append(vehicle.getInsuranceRate()).append(",\n");
            json.append("      \"basePrice\": ").append(vehicle.getBasePrice()).append(",\n");
            json.append("      \"longTermDiscounts\": {");
            
            Map<Integer, Double> discounts = vehicle.getLongTermDiscounts();
            if (discounts != null && !discounts.isEmpty()) {
                int count = 0;
                for (Map.Entry<Integer, Double> entry : discounts.entrySet()) {
                    if (count > 0) json.append(",");
                    json.append("\"").append(entry.getKey()).append("\": ").append(entry.getValue());
                    count++;
                }
            }
            json.append("}\n");
            json.append("    },\n");
            
            // 添加用户名字段
            json.append("    \"username\": \"").append(escapeJson(rental.getUsername() != null ? rental.getUsername() : "")).append("\",\n");
            json.append("    \"dueSoonReminderSent\": ").append(rental.isDueSoonReminderSent()).append(",\n");
            json.append("    \"overdueReminderSent\": ").append(rental.isOverdueReminderSent()).append(",\n");
            
            // 添加票据信息字段
            json.append("    \"ticket\": ");
            Ticket ticket = ticketService.getTicketByRentalId(rental.getId());
            if (ticket != null) {
                json.append("{\n");
                json.append("      \"ticketId\": \"").append(ticket.getTicketId()).append("\",\n");
                json.append("      \"generatedTime\": \"").append(ticket.getGeneratedTime()).append("\",\n");
                json.append("      \"pickupLocation\": \"").append(ticket.getPickupLocation()).append("\",\n");
                json.append("      \"specialInstructions\": \"").append(ticket.getSpecialInstructions()).append("\",\n");
                json.append("      \"isUsed\": ").append(ticket.isUsed()).append("\n");
                json.append("    }");
            } else {
                json.append("null");
            }
            json.append("\n");
            json.append("  }");
            
            if (i < rentals.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("]");
        return json.toString();
    }

    /**
     * 从JSON字符串中提取指定键的值
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return null;
        
        startIndex += searchKey.length();
        
        // 跳过空格
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }
        
        if (startIndex >= json.length()) return null;
        
        // 检查值类型
        char firstChar = json.charAt(startIndex);
        if (firstChar == '"') {
            // 字符串值
            startIndex++; // 跳过开始引号
            int endIndex = json.indexOf('"', startIndex);
            if (endIndex == -1) return null;
            return json.substring(startIndex, endIndex);
        } else {
            // 数字或布尔值
            int endIndex = startIndex;
            while (endIndex < json.length() && 
                   json.charAt(endIndex) != ',' && 
                   json.charAt(endIndex) != '}' && 
                   json.charAt(endIndex) != ']' &&
                   !Character.isWhitespace(json.charAt(endIndex))) {
                endIndex++;
            }
            return json.substring(startIndex, endIndex);
        }
    }

    /**
     * 从JSON字符串中提取指定键的对象值
     */
    private String extractJsonObject(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return null;
        
        startIndex += searchKey.length();
        
        // 跳过空格
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }
        
        if (startIndex >= json.length() || json.charAt(startIndex) != '{') return null;
        
        // 找到匹配的结束括号
        int braceCount = 0;
        int endIndex = startIndex;
        while (endIndex < json.length()) {
            char c = json.charAt(endIndex);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            
            endIndex++;
            if (braceCount == 0) break;
        }
        
        return json.substring(startIndex, endIndex);
    }

    /**
     * 分割JSON数组中的对象
     */
    private String[] splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        int start = 0;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (braceCount == 0) start = i;
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    objects.add(json.substring(start, i + 1));
                }
            }
        }
        
        return objects.toArray(new String[0]);
    }

    /**
     * Login validation, returns Account object or null
     */
    public Account login(String username, String password) {
        for (Account acc : accounts) {
            if (acc.getUsername().equals(username) && acc.getPassword().equals(password)) {
                return acc;
            }
        }
        return null;
    }

    /**
     * Find account by username
     */
    public Account findAccountByUsername(String username) {
        for (Account acc : accounts) {
            if (acc.getUsername().equals(username)) {
                return acc;
            }
        }
        return null;
    }

    /**
     * Find vehicle by ID
     */
    public Vehicle findVehicleById(int id) {
        for (Vehicle v : vehicles) {
            if (v.getId() == id) {
                return v;
            }
        }
        return null;
    }

    /**
     * Find customer by name (create if not exists)
     */
    public Customer findOrCreateCustomer(String name, String contact) {
        for (Customer c : customers) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        Customer newCustomer = new Customer(nextCustomerId++, name, contact);
        customers.add(newCustomer);
        return newCustomer;
    }

    /**
     * Check for rental conflicts (same vehicle, overlapping dates with buffer) and return conflict details
     */
    public String getConflictDetails(int vehicleId, LocalDate startDate, LocalDate endDate) {
        for (Rental r : rentals) {
            if (r.getVehicle().getId() == vehicleId && 
                (r.getStatus() == RentalStatus.ACTIVE || r.getStatus() == RentalStatus.PENDING)) {
                
                // Include 2-day buffer periods
                LocalDate bufferStart = r.getStartDate().minusDays(2);
                LocalDate bufferEnd = r.getEndDate().plusDays(2);
                
                // Check date overlap with buffer
                if (!(endDate.isBefore(bufferStart) || startDate.isAfter(bufferEnd))) {
                    String customerInfo = r.getCustomer().getName();
                    String statusText = r.getStatus() == RentalStatus.ACTIVE ? "ACTIVE" : "PENDING";
                    return String.format("Conflict with %s rental by %s (ID: %d) from %s to %s (with 2-day buffer: %s to %s)", 
                                       statusText, customerInfo, r.getId(), 
                                       r.getStartDate(), r.getEndDate(), bufferStart, bufferEnd);
                }
            }
        }
        return null; // No conflict
    }
    
    /**
     * Check for rental conflicts (same vehicle, overlapping dates with buffer)
     */
    public boolean hasConflict(int vehicleId, LocalDate startDate, LocalDate endDate) {
        return getConflictDetails(vehicleId, startDate, endDate) != null;
    }

    /**
     * Calculate rental fee with insurance and long-term discounts
     */
    public double calculateRentalFee(Vehicle vehicle, LocalDate startDate, LocalDate endDate, boolean insurance) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double baseRate = vehicle.getBasePrice(); // 使用车辆的基础价格
        double totalFee = baseRate * days;
        
        // Apply long-term discount
        double discount = 0.0;
        Map<Integer, Double> discounts = vehicle.getLongTermDiscounts();
        if (discounts != null) {
            for (Map.Entry<Integer, Double> entry : discounts.entrySet()) {
                if (days >= entry.getKey()) {
                    discount = Math.max(discount, entry.getValue());
                }
            }
        }
        totalFee = totalFee * (1 - discount);
        
        // Add insurance if selected
        if (insurance) {
            totalFee += totalFee * vehicle.getInsuranceRate();
        }
        
        return totalFee;
    }

    /**
     * Create a new rental
     */
    public Rental createRental(Customer customer, Vehicle vehicle, LocalDate startDate, LocalDate endDate, boolean insurance, String username) {
        double fee = calculateRentalFee(vehicle, startDate, endDate, insurance);
        Rental rental = new Rental(nextRentalId++, customer, vehicle, startDate, endDate, RentalStatus.PENDING, fee, insurance, username);
        rentals.add(rental);
        
        // 更新车辆状态为预留
        vehicle.setStatus(VehicleStatus.RESERVED);
        
        // 发送租赁确认通知
        notificationService.sendRentalConfirmation(username, vehicle.getModel(), 
                                                 startDate.toString(), endDate.toString(), fee);
        
        saveRentals("rentals.json"); // 立即保存租车记录
        return rental;
    }

    /**
     * Approve a rental (admin function)
     */
    public boolean approveRental(int rentalId) {
        Rental rental = findRentalById(rentalId);
        if (rental != null && rental.getStatus() == RentalStatus.PENDING) {
            rental.setStatus(RentalStatus.ACTIVE);
            rental.getVehicle().setStatus(VehicleStatus.RENTED);
            
            // Generate ticket for approved rental
            Ticket ticket = ticketService.generateTicket(rental);
            
            // Generate PDF ticket
            PdfTicketService pdfTicketService = new PdfTicketService();
            byte[] pdfTicket = pdfTicketService.generatePdfTicket(ticket);
            
            if (pdfTicket != null) {
                // Send rental approval notification with PDF ticket
                notificationService.sendRentalApprovalWithPdfTicket(rental.getUsername(), 
                                                     rental.getVehicle().getModel(), 
                                                               String.valueOf(rental.getId()),
                                                               ticket.getTicketId(),
                                                               pdfTicket);
                
                System.out.println(" PDF ticket generated and sent to customer email!");
            } else {
                // Fallback to regular ticket notification
                notificationService.sendRentalApprovalWithTicket(rental.getUsername(), 
                                                 rental.getVehicle().getModel(), 
                                                               String.valueOf(rental.getId()),
                                                               ticket.getTicketId());
                
                System.out.println(" PDF generation failed, sent regular ticket notification");
            }
            
            // Display ticket information to admin
            System.out.println("\n=== Ticket Generated ===");
            ticket.displayTicket();
            
            // Save data to JSON file
            saveRentals("rentals.json");
            saveVehicles("vehicles.json"); // 保存车辆状态变化
            
            return true;
        }
        return false;
    }

    /**
     * Cancel a rental
     */
    public boolean cancelRental(int rentalId) {
        return cancelRental(rentalId, "No reason provided");
    }
    
    public boolean cancelRental(int rentalId, String reason) {
        Rental rental = findRentalById(rentalId);
        if (rental != null && rental.getStatus() == RentalStatus.PENDING) {
            rental.setStatus(RentalStatus.CANCELLED);
            
            // 从车辆预订列表中移除这个预订
            rental.getVehicle().removeBooking(rental.getStartDate(), rental.getEndDate());
            rental.getVehicle().setStatus(VehicleStatus.AVAILABLE);
            
            
            // 立即保存数据到JSON文件
            saveRentals("rentals.json");
            saveVehicles("vehicles.json"); // 保存车辆状态变化
            
            return true;
        }
        return false;
    }

    /**
     * Return a vehicle
     */
    public boolean returnVehicle(int rentalId) {
        Rental rental = findRentalById(rentalId);
        if (rental != null && rental.getStatus() == RentalStatus.ACTIVE) {
            // Calculate actual fee based on actual return date
            double actualFee = calculateActualRentalFee(rental);
            rental.setActualFee(actualFee);
            
            rental.setStatus(RentalStatus.RETURNED);
            
            // 从车辆预订列表中移除这个预订
            rental.getVehicle().removeBooking(rental.getStartDate(), rental.getEndDate());
            rental.getVehicle().setStatus(VehicleStatus.AVAILABLE);
            
            saveRentals("rentals.json");
            saveVehicles("vehicles.json"); // 保存车辆状态变化
            return true;
        }
        return false;
    }
    
    /**
     * Calculate actual rental fee based on actual return date
     */
    public double calculateActualRentalFee(Rental rental) {
        LocalDate actualEndDate = LocalDate.now();
        LocalDate originalStartDate = rental.getStartDate();
        
        // Use the later of original end date or actual return date
        LocalDate effectiveEndDate = actualEndDate.isAfter(rental.getEndDate()) ? actualEndDate : rental.getEndDate();
        
        return calculateRentalFee(rental.getVehicle(), originalStartDate, effectiveEndDate, rental.isInsuranceSelected());
    }

    /**
     * Find rental by ID
     */
    public Rental findRentalById(int id) {
        for (Rental r : rentals) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    /**
     * Get rentals by customer name
     */
    public List<Rental> getRentalsByCustomer(String customerName) {
        List<Rental> result = new ArrayList<>();
        for (Rental r : rentals) {
            if (r.getCustomer().getName().equals(customerName)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Get rentals by username (for logged in user)
     */
    public List<Rental> getRentalsByUsername(String username) {
        List<Rental> result = new ArrayList<>();
        for (Rental r : rentals) {
            if (r.getUsername() != null && r.getUsername().equals(username)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Get all pending rentals (for admin approval)
     */
    public List<Rental> getPendingRentals() {
        List<Rental> result = new ArrayList<>();
        for (Rental r : rentals) {
            if (r.getStatus() == RentalStatus.PENDING) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Get all active rentals
     */
    public List<Rental> getActiveRentals() {
        List<Rental> result = new ArrayList<>();
        for (Rental r : rentals) {
            if (r.getStatus() == RentalStatus.ACTIVE) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * 搜索车辆 - 按车牌号
     */
    public List<Vehicle> searchVehiclesByCarPlate(String carPlate) {
        List<Vehicle> results = new ArrayList<>();
        String searchTerm = carPlate.toLowerCase().trim();
        
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getCarPlate().toLowerCase().contains(searchTerm)) {
                results.add(vehicle);
            }
        }
        return results;
    }
    
    /**
     * 搜索车辆 - 按品牌
     */
    public List<Vehicle> searchVehiclesByBrand(String brand) {
        List<Vehicle> results = new ArrayList<>();
        String searchTerm = brand.toLowerCase().trim();
        
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getBrand().toLowerCase().contains(searchTerm)) {
                results.add(vehicle);
            }
        }
        return results;
    }
    
    /**
     * 搜索车辆 - 按型号
     */
    public List<Vehicle> searchVehiclesByModel(String model) {
        List<Vehicle> results = new ArrayList<>();
        String searchTerm = model.toLowerCase().trim();
        
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getModel().toLowerCase().contains(searchTerm)) {
                results.add(vehicle);
            }
        }
        return results;
    }
    
    /**
     * 搜索车辆 - 按车辆类型
     */
    public List<Vehicle> searchVehiclesByType(VehicleType vehicleType) {
        List<Vehicle> results = new ArrayList<>();
        
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getVehicleType() == vehicleType) {
                results.add(vehicle);
            }
        }
        return results;
    }
    
    /**
     * 搜索车辆 - 按燃料类型
     */
    public List<Vehicle> searchVehiclesByFuelType(FuelType fuelType) {
        List<Vehicle> results = new ArrayList<>();
        
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getFuelType() == fuelType) {
                results.add(vehicle);
            }
        }
        return results;
    }
    
    /**
     * 综合搜索 - 可以同时按多个条件搜索
     */
    public List<Vehicle> searchVehicles(String carPlate, String brand, String model, 
                                      VehicleType vehicleType, FuelType fuelType, boolean onlyAvailable) {
        List<Vehicle> results = new ArrayList<>();
        
        for (Vehicle vehicle : vehicles) {
            boolean matches = true;
            
            // 如果指定了只显示可用车辆
            if (onlyAvailable && vehicle.getStatus() != VehicleStatus.AVAILABLE) {
                continue;
            }
            
            // 检查车牌号
            if (carPlate != null && !carPlate.trim().isEmpty()) {
                if (!vehicle.getCarPlate().toLowerCase().contains(carPlate.toLowerCase().trim())) {
                    matches = false;
                }
            }
            
            // 检查品牌
            if (brand != null && !brand.trim().isEmpty()) {
                if (!vehicle.getBrand().toLowerCase().contains(brand.toLowerCase().trim())) {
                    matches = false;
                }
            }
            
            // 检查型号
            if (model != null && !model.trim().isEmpty()) {
                if (!vehicle.getModel().toLowerCase().contains(model.toLowerCase().trim())) {
                    matches = false;
                }
            }
            
            // 检查车辆类型
            if (vehicleType != null && vehicle.getVehicleType() != vehicleType) {
                matches = false;
            }
            
            // 检查燃料类型
            if (fuelType != null && vehicle.getFuelType() != fuelType) {
                matches = false;
            }
            
            if (matches) {
                results.add(vehicle);
            }
        }
        
        return results;
    }
    
    /**
     * 快速搜索 - 在车牌、品牌、型号中同时搜索关键词
     */
    public List<Vehicle> quickSearchVehicles(String keyword, boolean onlyAvailable) {
        List<Vehicle> results = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return onlyAvailable ? getAvailableVehicles() : vehicles;
        }
        
        String searchTerm = keyword.toLowerCase().trim();
        
        for (Vehicle vehicle : vehicles) {
            // 如果指定了只显示可用车辆
            if (onlyAvailable && vehicle.getStatus() != VehicleStatus.AVAILABLE) {
                continue;
            }
            
            // 在车牌、品牌、型号中搜索
            if (vehicle.getCarPlate().toLowerCase().contains(searchTerm) ||
                vehicle.getBrand().toLowerCase().contains(searchTerm) ||
                vehicle.getModel().toLowerCase().contains(searchTerm)) {
                results.add(vehicle);
            }
        }
        
        return results;
    }

    // Getters
    public List<Account> getAccounts() {
        return accounts;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public List<Rental> getRentals() {
        return rentals;
    }

    public List<Customer> getCustomers() {
        return customers;
    }
    
    // 获取通知服务
    public NotificationService getNotificationService() {
        return notificationService;
    }
    
    public TicketService getTicketService() {
        return ticketService;
    }
    
    // 检查并发送提醒
    public void checkAndSendReminders() {
        notificationService.checkAndSendReminders(rentals);
    }
    
    // 获取用户消息
    public List<Message> getUserMessages(String username) {
        return notificationService.getUserMessages(username);
    }
    
    // 获取未读消息数量
    public int getUnreadMessageCount(String username) {
        return notificationService.getUnreadMessages(username).size();
    }
    
    // 标记消息为已读
    public boolean markMessageAsRead(String messageId) {
        return notificationService.markMessageAsRead(messageId);
    }
    
    // 发送用户消息
    public boolean sendUserMessage(String fromUser, String toUser, String subject, String content) {
        return notificationService.sendUserMessage(fromUser, toUser, subject, content);
    }
    
    // 生成报告（增强版）
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 租赁系统报告 ===\n");
        report.append("生成时间: ").append(LocalDate.now()).append("\n\n");
        
        // 车辆统计
        report.append("车辆统计:\n");
        Map<VehicleStatus, Long> vehicleStats = vehicles.stream()
            .collect(java.util.stream.Collectors.groupingBy(Vehicle::getStatus, 
                    java.util.stream.Collectors.counting()));
        
        for (Map.Entry<VehicleStatus, Long> entry : vehicleStats.entrySet()) {
            report.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        // 租赁统计
        report.append("\n租赁统计:\n");
        Map<RentalStatus, Long> rentalStats = rentals.stream()
            .collect(java.util.stream.Collectors.groupingBy(Rental::getStatus, 
                    java.util.stream.Collectors.counting()));
        
        for (Map.Entry<RentalStatus, Long> entry : rentalStats.entrySet()) {
            report.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        // 收入统计
        report.append("\n收入统计:\n");
        double totalRevenue = rentals.stream()
            .filter(r -> r.getStatus() == RentalStatus.RETURNED)
            .mapToDouble(Rental::getActualFee)
            .sum();
        
        double pendingRevenue = rentals.stream()
            .filter(r -> r.getStatus() == RentalStatus.PENDING || r.getStatus() == RentalStatus.ACTIVE)
            .mapToDouble(Rental::getTotalFee)
            .sum();
        
        report.append("  Lease income completed: RM").append(String.format("%.2f", totalRevenue)).append("\n");
        report.append("  Lease income to be completed: RM").append(String.format("%.2f", pendingRevenue)).append("\n");
        report.append("  Total: RM").append(String.format("%.2f", totalRevenue + pendingRevenue)).append("\n");
        
        // 逾期租赁
        report.append("\nOverdue rentals:\n");
        List<Rental> overdueRentals = rentals.stream()
            .filter(r -> r.getStatus() == RentalStatus.ACTIVE && r.isOverdue())
            .collect(java.util.stream.Collectors.toList());
        
        if (overdueRentals.isEmpty()) {
            report.append("  No overdue leases\n");
        } else {
            for (Rental rental : overdueRentals) {
                report.append("  Rental ID: ").append(rental.getId())
                      .append(", Username: ").append(rental.getUsername())
                      .append(", Vehicle: ").append(rental.getVehicle().getModel())
                      .append(", End Date: ").append(rental.getEndDate())
                      .append("\n");
            }
        }
        
        return report.toString();
    }
    
    // 导出数据（增强版）
    public boolean exportData(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("=== Leasing system data export ===");
            writer.println("Export time: " + LocalDate.now());
            writer.println();
            
            // 导出车辆数据
            writer.println("=== Vehicle data ===");
            for (Vehicle vehicle : vehicles) {
                writer.println("ID: " + vehicle.getId() + 
                             ",Modal: " + vehicle.getModel() + 
                             ", Status: " + vehicle.getStatus() + 
                             ", Price: RM" + vehicle.getBasePrice() + "/day");
            }
            writer.println();
            
            // 导出租赁数据
            writer.println("=== Leasing data ===");
            for (Rental rental : rentals) {
                writer.println("ID: " + rental.getId() + 
                             ", Username: " + rental.getUsername() + 
                             ", Vehicle: " + rental.getVehicle().getModel() + 
                             ", Status: " + rental.getStatus() + 
                             ", Start Date: " + rental.getStartDate() + 
                             ", End Date: " + rental.getEndDate() + 
                             ", Fee: RM" + rental.getTotalFee());
            }
            writer.println();
            
            // 导出用户数据
            writer.println("=== User data ===");
            for (Account account : accounts) {
                writer.println("Username: " + account.getUsername() + 
                             ", Role: " + account.getRole());
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("Export data failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 添加车辆维护记录
     */
    public boolean addMaintenanceLog(int vehicleId, MaintenanceLogType logType, String description, 
                                   String reportedBy, int severityLevel) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle != null) {
            MaintenanceLog log = new MaintenanceLog(vehicleId, logType, description, reportedBy, severityLevel);
            vehicle.addMaintenanceLog(log);
            
            // 如果是高严重程度问题，自动发送通知给所有管理员
            if (severityLevel >= 4) {
                sendCriticalMaintenanceNotification(vehicle, description, severityLevel, reportedBy);
            }
            
            // 自动保存维护记录到JSON文件
            saveMaintenanceLogs("maintenance_logs.json");
            
            // 保存车辆状态变化到JSON文件
            saveVehicles("vehicles.json");
            
            return true;
        }
        return false;
    }

    /**
     * 发送高严重程度维护通知给管理员
     */
    private void sendCriticalMaintenanceNotification(Vehicle vehicle, String description, 
                                                   int severity, String reportedBy) {
        String subject = String.format("CRITICAL MAINTENANCE ALERT - Vehicle %d", vehicle.getId());
        String content = String.format(
            " HIGH PRIORITY MAINTENANCE ISSUE \n\n" +
            "Vehicle: %s %s (ID: %d)\n" +
            "Car Plate: %s\n" +
            "Severity Level: %d/5\n" +
            "Reported By: %s\n" +
            "Issue Description: %s\n\n" +
            "This issue requires immediate attention due to its high severity level.\n" +
            "Please address this maintenance issue as soon as possible.",
            vehicle.getBrand(), vehicle.getModel(), vehicle.getId(),
            vehicle.getCarPlate(), severity, reportedBy, description
        );
        
        // 发送给所有管理员
        for (Account account : accounts) {
            if (account.getRole() == AccountRole.ADMIN) {
                notificationService.sendUserMessage("SYSTEM", account.getUsername(), subject, content);
            }
        }
    }
    
    /**
     * 解决维护问题
     */
    public boolean resolveMaintenanceLog(int vehicleId, int logId, double cost, String assignedTo) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle != null) {
            for (MaintenanceLog log : vehicle.getMaintenanceLogs()) {
                if (log.getId() == logId) {
                    log.setStatus(MaintenanceStatus.RESOLVED);
                    log.setCost(cost);
                    log.setAssignedTo(assignedTo);
                    vehicle.resolveMaintenanceLog(logId, cost);
                    
                    // 自动保存维护记录到JSON文件
                    saveMaintenanceLogs("maintenance_logs.json");
                    
                    // 保存车辆状态变化到JSON文件
                    saveVehicles("vehicles.json");
                    
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 获取所有未解决的维护问题
     */
    public List<MaintenanceLog> getAllUnresolvedMaintenanceLogs() {
        List<MaintenanceLog> allLogs = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            allLogs.addAll(vehicle.getUnresolvedMaintenanceLogs());
        }
        return allLogs;
    }
    
    /**
     * 获取特定车辆的维护历史
     */
    public List<MaintenanceLog> getVehicleMaintenanceHistory(int vehicleId) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle != null) {
            return vehicle.getMaintenanceLogs();
        }
        return new ArrayList<>();
    }
    
    /**
     * 检查车辆在指定时间段的可用性（包括缓冲期）
     */
    public boolean isVehicleAvailable(int vehicleId, LocalDate startDate, LocalDate endDate) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle != null) {
            return vehicle.isAvailable(startDate, endDate);
        }
        return false;
    }
    
    /**
     * 获取车辆的不可用时间段（包括租赁信息）
     */
    public List<String> getVehicleUnavailablePeriods(int vehicleId) {
        List<String> periods = new ArrayList<>();
        
        // 获取所有活跃和待审批的租赁
        for (Rental rental : rentals) {
            if (rental.getVehicle().getId() == vehicleId && 
                (rental.getStatus() == RentalStatus.ACTIVE || rental.getStatus() == RentalStatus.PENDING)) {
                
                LocalDate bufferStart = rental.getStartDate().minusDays(2);
                LocalDate bufferEnd = rental.getEndDate().plusDays(2);
                
                String statusText = rental.getStatus() == RentalStatus.ACTIVE ? "ACTIVE" : "PENDING";
                String customerInfo = rental.getCustomer().getName();
                
                periods.add(String.format("%s to %s (includes 2-day buffer) - %s by %s", 
                           bufferStart, bufferEnd, statusText, customerInfo));
            }
        }
        
        return periods;
    }
    
    /**
     * 创建租赁（更新版本，支持新的车辆预定系统）
     */
    public Rental createRentalWithSchedule(Customer customer, Vehicle vehicle, LocalDate startDate, 
                                         LocalDate endDate, boolean insurance, String username) {
        // 检查车辆是否可用（包括缓冲期检查）
        if (!vehicle.isAvailable(startDate, endDate)) {
            throw new IllegalArgumentException("Vehicle is not available for the requested period");
        }
        
        double fee = calculateRentalFee(vehicle, startDate, endDate, insurance);
        Rental rental = new Rental(nextRentalId++, customer, vehicle, startDate, endDate, 
                                 RentalStatus.PENDING, fee, insurance, username);
        rentals.add(rental);
        
        // 添加到车辆的预定时间表
        vehicle.addBooking(startDate, endDate);
        
        // 更新车辆状态为预留
        vehicle.setStatus(VehicleStatus.RESERVED);
        
        // Send booking request notification to admin only
        // notificationService.sendAdminNotification("New Rental Request", 
        //                                          "New rental request from " + username + 
        //                                          " for " + vehicle.getModel() + 
        //                                          " (Rental ID: " + rental.getId() + ")");
        
        saveRentals("rentals.json");
        return rental;
    }
    
    /**
     * 车辆返还时进行损坏检查
     */
    public boolean returnVehicleWithDamageCheck(int rentalId, String customerName, 
                                              List<String> damageReports) {
        Rental rental = findRentalById(rentalId);
        if (rental != null && rental.getStatus() == RentalStatus.ACTIVE) {
            // 计算实际费用
            double actualFee = calculateActualRentalFee(rental);
            rental.setActualFee(actualFee);
            
            // 处理损坏报告
            if (damageReports != null && !damageReports.isEmpty()) {
                for (String damage : damageReports) {
                    addMaintenanceLog(rental.getVehicle().getId(), MaintenanceLogType.DAMAGE_REPORT, 
                                    damage, customerName, 3); // 默认严重程度为3
                }
                System.out.println("Damage reports filed: " + damageReports.size() + " issues reported.");
            }
            
            // 设置租赁状态为已返还
            rental.setStatus(RentalStatus.RETURNED);
            
            // 从车辆预订列表中移除这个预订
            Vehicle vehicle = rental.getVehicle();
            vehicle.removeBooking(rental.getStartDate(), rental.getEndDate());
            
            // 检查是否有严重维护问题
            if (vehicle.hasCriticalMaintenanceIssues()) {
                vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
                System.out.println("Vehicle " + vehicle.getId() + " has been placed under maintenance due to reported issues.");
            } else {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
            }
            
            saveRentals("rentals.json");
            
            // 同步车辆状态以确保一致性
            syncVehicleStatusWithRentals();
            saveVehicles("vehicles.json"); // 保存车辆状态变化
            return true;
        }
        return false;
    }

    /**
     * 获取需要维护的车辆列表
     */
    public List<Vehicle> getVehiclesNeedingMaintenance() {
        List<Vehicle> needMaintenance = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.hasCriticalMaintenanceIssues() || 
                vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE) {
                needMaintenance.add(vehicle);
            }
        }
        return needMaintenance;
    }
    
    /**
     * 获取所有可用车辆（考虑维护状态）
     */
    public List<Vehicle> getAvailableVehicles() {
        List<Vehicle> available = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getStatus() == VehicleStatus.AVAILABLE || vehicle.getStatus() == VehicleStatus.RESERVED &&
                !vehicle.hasCriticalMaintenanceIssues()) {
                available.add(vehicle);
            }
        }
        return available;
    }

    /**
     * 获取所有维护日志
     */
    public List<MaintenanceLog> getAllMaintenanceLogs() {
        List<MaintenanceLog> allLogs = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            allLogs.addAll(vehicle.getMaintenanceLogs());
        }
        return allLogs;
    }
    
    /**
     * 根据ID查找维护日志
     */
    public MaintenanceLog findMaintenanceLogById(int logId) {
        for (Vehicle vehicle : vehicles) {
            for (MaintenanceLog log : vehicle.getMaintenanceLogs()) {
                if (log.getId() == logId) {
                    return log;
                }
            }
        }
        return null;
    }
    
    /**
     * 获取特定车辆的维护日志
     */
    public List<MaintenanceLog> getMaintenanceLogsByVehicleId(int vehicleId) {
        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle != null) {
            return vehicle.getMaintenanceLogs();
        }
        return new ArrayList<>();
    }

    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalRentals = rentals.size();
        long activeRentals = rentals.stream().filter(r -> r.getStatus() == RentalStatus.ACTIVE).count();
        long completedRentals = rentals.stream().filter(r -> r.getStatus() == RentalStatus.RETURNED).count();
        long pendingRentals = rentals.stream().filter(r -> r.getStatus() == RentalStatus.PENDING).count();
        double totalRevenue = rentals.stream()
            .filter(r -> r.getStatus() == RentalStatus.RETURNED)
            .mapToDouble(Rental::getTotalFee)
            .sum();

        long totalVehicles = vehicles.size();
        long availableVehicles = vehicles.stream().filter(v -> v.getStatus() == VehicleStatus.AVAILABLE).count();
        long rentedVehicles = vehicles.stream().filter(v -> v.getStatus() == VehicleStatus.RENTED).count();
        long maintenanceVehicles = vehicles.stream().filter(v -> v.getStatus() == VehicleStatus.UNDER_MAINTENANCE).count();
        
        stats.put("totalVehicles", totalVehicles);
        stats.put("availableVehicles", availableVehicles);
        stats.put("rentedVehicles", rentedVehicles);
        stats.put("maintenanceVehicles", maintenanceVehicles);
        stats.put("totalRentals", totalRentals);
        stats.put("activeRentals", activeRentals);
        stats.put("completedRentals", completedRentals);
        stats.put("pendingRentals", pendingRentals);
        stats.put("totalRevenue", totalRevenue);
        stats.put("averageRevenue", completedRentals > 0 ? totalRevenue / completedRentals : 0.0);
        
        List<String> headers = Arrays.asList("Metric", "Value");
        List<List<String>> data = Arrays.asList(
            Arrays.asList("Total Vehicles", String.valueOf(totalVehicles)),
            Arrays.asList("Available Vehicles", String.valueOf(availableVehicles)),
            Arrays.asList("Rented Vehicles", String.valueOf(rentedVehicles)),
            Arrays.asList("Maintenance Vehicles", String.valueOf(maintenanceVehicles)),
            Arrays.asList("Total Rentals", String.valueOf(totalRentals)),
            Arrays.asList("Active Rentals", String.valueOf(activeRentals)),
            Arrays.asList("Completed Rentals", String.valueOf(completedRentals)),
            Arrays.asList("Pending Rentals", String.valueOf(pendingRentals)),
            Arrays.asList("Total Revenue", String.format("RM%.2f", totalRevenue)),
            Arrays.asList("Average Revenue/Rental", String.format("RM%.2f", (completedRentals > 0 ? totalRevenue / completedRentals : 0.0)))
        );
        
        stats.put("exportHeaders", headers);
        stats.put("exportData", data);

        return stats;
    }
    
    /**
      * 解析维护记录JSON数据
      */
     private void parseMaintenanceLogsFromJson(String json) {
         try {
             json = json.trim();
             if (json.startsWith("[") && json.endsWith("]")) {
                 json = json.substring(1, json.length() - 1); // 移除外层[]
                 
                 String[] logObjects = splitJsonObjects(json);
                 
                 for (String logJson : logObjects) {
                     MaintenanceLog log = parseMaintenanceLogFromJson(logJson.trim());
                     if (log != null) {
                         // 找到对应的车辆并添加维护记录
                         Vehicle vehicle = findVehicleById(log.getVehicleId());
                         if (vehicle != null) {
                             vehicle.addMaintenanceLogDirect(log);
                         }
                     }
                 }
             }
             
             // 加载完维护记录后，更新所有车辆的状态
             for (Vehicle vehicle : vehicles) {
                 if (vehicle.hasCriticalMaintenanceIssues() && vehicle.getStatus() == VehicleStatus.AVAILABLE) {
                     vehicle.setStatus(VehicleStatus.UNDER_MAINTENANCE);
                 }
             }
             
             // 更新维护记录的下一个ID
             List<MaintenanceLog> allLogs = getAllMaintenanceLogs();
             int maxId = 0;
             for (MaintenanceLog log : allLogs) {
                 if (log.getId() > maxId) {
                     maxId = log.getId();
                 }
             }
             if (maxId > 0) {
                 MaintenanceLog.setNextId(maxId + 1);
             }
         } catch (Exception e) {
             System.out.println("Failed to parse maintenance logs JSON: " + e.getMessage());
         }
     }

     /**
      * 解析单个维护记录对象
      */
     private MaintenanceLog parseMaintenanceLogFromJson(String json) {
         try {
             int id = Integer.parseInt(extractJsonValue(json, "id"));
             int vehicleId = Integer.parseInt(extractJsonValue(json, "vehicleId"));
             String logTypeStr = extractJsonValue(json, "logType");
             String description = extractJsonValue(json, "description");
             String reportDateStr = extractJsonValue(json, "reportDate");
             String completedDateStr = extractJsonValue(json, "completedDate");
             double cost = Double.parseDouble(extractJsonValue(json, "cost"));
             String statusStr = extractJsonValue(json, "status");
             String reportedBy = extractJsonValue(json, "reportedBy");
             String assignedTo = extractJsonValue(json, "assignedTo");
             int severityLevel = Integer.parseInt(extractJsonValue(json, "severityLevel"));

             // 创建维护记录对象
             MaintenanceLog log = new MaintenanceLog(vehicleId, 
                 MaintenanceLogType.valueOf(logTypeStr), 
                 description, reportedBy, severityLevel);
             
             // 设置其他属性
             log.setId(id);
             log.setCost(cost);
             log.setStatus(MaintenanceStatus.valueOf(statusStr));
             log.setAssignedTo(assignedTo != null ? assignedTo : "");
             
             // 解析日期
             if (reportDateStr != null && !reportDateStr.isEmpty()) {
                 log.setReportDate(java.time.LocalDateTime.parse(reportDateStr));
             }
             if (completedDateStr != null && !completedDateStr.isEmpty() && !completedDateStr.equals("null")) {
                 log.setCompletedDate(java.time.LocalDateTime.parse(completedDateStr));
             }
             
             return log;
         } catch (Exception e) {
             System.out.println("Failed to parse maintenance log object: " + e.getMessage());
         }
         return null;
     }

     /**
      * 将维护记录转换为JSON格式
      */
     private String convertMaintenanceLogsToJson() {
         StringBuilder json = new StringBuilder();
         json.append("[\n");
         
         List<MaintenanceLog> allLogs = getAllMaintenanceLogs();
         
         for (int i = 0; i < allLogs.size(); i++) {
             MaintenanceLog log = allLogs.get(i);
             json.append("  {\n");
             json.append("    \"id\": ").append(log.getId()).append(",\n");
             json.append("    \"vehicleId\": ").append(log.getVehicleId()).append(",\n");
             json.append("    \"logType\": \"").append(log.getLogType()).append("\",\n");
             json.append("    \"description\": \"").append(escapeJson(log.getDescription())).append("\",\n");
             json.append("    \"reportDate\": \"").append(log.getReportDate()).append("\",\n");
             json.append("    \"completedDate\": ").append(log.getCompletedDate() != null ? 
                 "\"" + log.getCompletedDate() + "\"" : "null").append(",\n");
             json.append("    \"cost\": ").append(log.getCost()).append(",\n");
             json.append("    \"status\": \"").append(log.getStatus()).append("\",\n");
             json.append("    \"reportedBy\": \"").append(escapeJson(log.getReportedBy())).append("\",\n");
             json.append("    \"assignedTo\": \"").append(escapeJson(log.getAssignedTo() != null ? log.getAssignedTo() : "")).append("\",\n");
             json.append("    \"severityLevel\": ").append(log.getSeverityLevel()).append("\n");
             json.append("  }");
             
             if (i < allLogs.size() - 1) {
                 json.append(",");
             }
             json.append("\n");
         }
         
         json.append("]");
         return json.toString();
     }

     /**
      * 转义JSON字符串
      */
     private String escapeJson(String str) {
         if (str == null) return "";
         return str.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
     }

    /**
     * 同步租赁记录中的车辆引用（确保指向最新的车辆对象）
     */
    private void syncRentalVehicleReferences() {
        for (Rental rental : rentals) {
            Vehicle updatedVehicle = findVehicleById(rental.getVehicle().getId());
            if (updatedVehicle != null) {
                rental.setVehicle(updatedVehicle);
            }
        }
    }

    /**
     * 保存车辆数据到JSON文件
     */
    public void saveVehicles(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            String jsonContent = convertVehiclesToJson();
            writer.println(jsonContent);
        } catch (IOException e) {
            System.out.println("Failed to save vehicle data: " + e.getMessage());
        }
    }

    /**
     * 将车辆数据转换为JSON格式
     */
    private String convertVehiclesToJson() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            json.append("  {\n");
            json.append("    \"id\": ").append(vehicle.getId()).append(",\n");
            json.append("    \"brand\": \"").append(vehicle.getBrand()).append("\",\n");
            json.append("    \"model\": \"").append(vehicle.getModel()).append("\",\n");
            json.append("    \"carPlate\": \"").append(vehicle.getCarPlate()).append("\",\n");
            json.append("    \"vehicleType\": \"").append(vehicle.getVehicleType()).append("\",\n");
            json.append("    \"fuelType\": \"").append(vehicle.getFuelType()).append("\",\n");
            json.append("    \"status\": \"").append(vehicle.getStatus()).append("\",\n");
            json.append("    \"insuranceRate\": ").append(vehicle.getInsuranceRate()).append(",\n");
            json.append("    \"basePrice\": ").append(vehicle.getBasePrice()).append(",\n");
            json.append("    \"longTermDiscounts\": {");
            
            if (vehicle.getLongTermDiscounts() != null && !vehicle.getLongTermDiscounts().isEmpty()) {
                java.util.Iterator<java.util.Map.Entry<Integer, Double>> iterator = 
                    vehicle.getLongTermDiscounts().entrySet().iterator();
                while (iterator.hasNext()) {
                    java.util.Map.Entry<Integer, Double> entry = iterator.next();
                    json.append("\"").append(entry.getKey()).append("\": ").append(entry.getValue());
                    if (iterator.hasNext()) {
                        json.append(", ");
                    }
                }
            }
            
            json.append("}\n");
            json.append("  }");
            
            if (i < vehicles.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("]");
        return json.toString();
    }

    /**
     * Check if this is a rental extension by the same user (no buffer needed)
     */
    public boolean isRentalExtension(int vehicleId, LocalDate startDate, LocalDate endDate, String username) {
        for (Rental r : rentals) {
            if (r.getVehicle().getId() == vehicleId && 
                r.getUsername().equals(username) &&
                (r.getStatus() == RentalStatus.ACTIVE || r.getStatus() == RentalStatus.PENDING)) {
                
                // Check if the new booking is adjacent or overlapping with existing booking
                if (startDate.equals(r.getEndDate().plusDays(1)) || 
                    endDate.equals(r.getStartDate().minusDays(1)) ||
                    (!startDate.isAfter(r.getEndDate()) && !endDate.isBefore(r.getStartDate()))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Enhanced conflict check that considers rental extensions
     */
    public String getConflictDetailsWithExtension(int vehicleId, LocalDate startDate, LocalDate endDate, String username) {
        // If this is a rental extension, use less strict checking
        if (isRentalExtension(vehicleId, startDate, endDate, username)) {
            Vehicle vehicle = findVehicleById(vehicleId);
            if (vehicle != null && vehicle.isAvailableForExtension(startDate, endDate, username)) {
                return null; // No conflict for extension
            }
        }
        
        // Use normal conflict checking for new bookings
        return getConflictDetails(vehicleId, startDate, endDate);
    }

    /**
     * Search and display user accounts (for admin offline booking)
     */
    public List<Account> searchUserAccounts(String searchTerm) {
        List<Account> results = new ArrayList<>();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // Return all customer accounts if no search term
            for (Account account : accounts) {
                if (account.getRole() == AccountRole.CUSTOMER) {
                    results.add(account);
                }
            }
            return results;
        }
        
        String searchLower = searchTerm.toLowerCase().trim();
        for (Account account : accounts) {
            if (account.getRole() == AccountRole.CUSTOMER) {
                if (account.getUsername().toLowerCase().contains(searchLower) ||
                    (account.getFullName() != null && account.getFullName().toLowerCase().contains(searchLower)) ||
                    (account.getContactNumber() != null && account.getContactNumber().contains(searchTerm))) {
                    results.add(account);
                }
            }
        }
        return results;
    }
    
    /**
     * Get account by username
     */
    public Account getAccountByUsername(String username) {
        for (Account account : accounts) {
            if (account.getUsername().equals(username)) {
                return account;
            }
        }
        return null;
    }

    /**
     * Create a rental directly as ACTIVE (for admin offline booking)
     */
    public Rental createOfflineRental(Customer customer, Vehicle vehicle, LocalDate startDate, 
                                    LocalDate endDate, boolean insurance, String username) {
        // Check if vehicle is available
        String conflictDetails = getConflictDetailsWithExtension(vehicle.getId(), startDate, endDate, username);
        if (conflictDetails != null) {
            throw new IllegalArgumentException("Vehicle conflict: " + conflictDetails);
        }
        
        double fee = calculateRentalFee(vehicle, startDate, endDate, insurance);
        Rental rental = new Rental(nextRentalId++, customer, vehicle, startDate, endDate, 
                                 RentalStatus.ACTIVE, fee, insurance, username);
        rentals.add(rental);
     
        // Add to vehicle schedule
        vehicle.addBooking(startDate, endDate);
        
        // Set vehicle status to RENTED
        vehicle.setStatus(VehicleStatus.RENTED);
        
        // Generate ticket immediately
        Ticket ticket = ticketService.generateTicket(rental);
        
        // Send confirmation message to customer
        // notificationService.sendRentalApprovalWithTicket(username, vehicle.getModel(), 
        //                                                String.valueOf(rental.getId()),
        //                                                ticket.getTicketId());
        
        // Save data
        saveRentals("rentals.json");
        saveVehicles("vehicles.json");
        
        return rental;
    }

    /**
     * Find active rental by user and vehicle (enhanced matching)
     */
    public Rental findActiveRentalByUserAndVehicle(String username, int vehicleId) {
        // First, try to find the account to get the full name
        Account account = getAccountByUsername(username);
        String accountFullName = (account != null) ? account.getFullName() : null;
        
        for (Rental rental : rentals) {
            if (rental.getVehicle().getId() == vehicleId &&
                rental.getStatus() == RentalStatus.ACTIVE) {
                
                // Check username match
                if (rental.getUsername() != null && rental.getUsername().equals(username)) {
                    return rental;
                }
                
                // Check customer name match with account full name
                if (accountFullName != null && !accountFullName.isEmpty() &&
                    rental.getCustomer() != null && 
                    rental.getCustomer().getName().equals(accountFullName)) {
                    return rental;
                }
                
                // Check if username matches customer name (fallback)
                if (rental.getCustomer() != null && 
                    rental.getCustomer().getName().equals(username)) {
                    return rental;
                }
            }
        }
        return null;
    }
    
    /**
     * Extend existing rental
     */
    public boolean extendRental(String username, int vehicleId, LocalDate newEndDate, boolean insurance) {
        Rental existingRental = findActiveRentalByUserAndVehicle(username, vehicleId);
        if (existingRental == null) {
            return false;
        }
        
        Vehicle vehicle = existingRental.getVehicle();
        LocalDate originalEndDate = existingRental.getEndDate();
        
        // Update vehicle booking schedule
        vehicle.removeBooking(existingRental.getStartDate(), originalEndDate);
        vehicle.addBooking(existingRental.getStartDate(), newEndDate);
        
        // Calculate new total fee
        double newTotalFee = calculateRentalFee(vehicle, existingRental.getStartDate(), newEndDate, insurance);
        
        // Update rental details
        existingRental.setEndDate(newEndDate);
        existingRental.setTotalFee(newTotalFee);
        existingRental.setInsuranceSelected(insurance);
        
        // Reset reminder flags for extended rental
        existingRental.setDueSoonReminderSent(false);
        existingRental.setOverdueReminderSent(false);
        
        // Generate new ticket for the extended rental
        Ticket newTicket = ticketService.generateTicket(existingRental);
        
        // Send extension notification
        // notificationService.sendRentalApprovalWithTicket(username, vehicle.getModel(), 
        //                                                String.valueOf(existingRental.getId()),
        //                                                newTicket.getTicketId());
        
        // Save data
        saveRentals("rentals.json");
        saveVehicles("vehicles.json");
        
        return true;
    }

    /**
     * 同步车辆状态与租赁状态，确保一致性
     */
    public void syncVehicleStatusWithRentals() {
        List<Rental> activeRentals = getActiveRentals();
        
        for (Vehicle vehicle : vehicles) {
            // 保护特殊状态：UNDER_MAINTENANCE 和 OUT_OF_SERVICE 不被覆盖
            if (vehicle.getStatus() == VehicleStatus.UNDER_MAINTENANCE || 
                vehicle.getStatus() == VehicleStatus.OUT_OF_SERVICE) {
                continue; // 跳过这些车辆，保持其状态不变
            }
            
            boolean hasActiveRental = false;
            
            // 检查是否有该车辆的活跃租赁
            for (Rental rental : activeRentals) {
                if (rental.getVehicle().getId() == vehicle.getId()) {
                    hasActiveRental = true;
                    break;
                }
            }
            
            // 如果没有活跃租赁但状态是RESERVED或RENTED，则设为AVAILABLE
            if (!hasActiveRental && 
                (vehicle.getStatus() == VehicleStatus.RESERVED || vehicle.getStatus() == VehicleStatus.RENTED)) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
            }
            
            // 如果有活跃租赁但状态是AVAILABLE，则设为RESERVED
            if (hasActiveRental && vehicle.getStatus() == VehicleStatus.AVAILABLE) {
                vehicle.setStatus(VehicleStatus.RESERVED);
            }
        }
    }
}