import java.io.*;
import java.net.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import com.sun.net.httpserver.*;

enum ReservationStatus{
    RESERVED, CANCELLED, LATEWITHDRAW, NOSHOW
}

/*============================ Member 類別 ================================= */
class Member{
    private final String studentId;
    private final String hashedPassword;
    private int violationTimes = 0;
    private final List<Reservation> historyReservations = new ArrayList<>();
    private LocalDate suspensionEndDate = null; // 記錄停權解除日期
    // ----------------- 建構式 -----------------
    public Member(String studentId, String hashedPassword){
        this.studentId = studentId;
        this.hashedPassword = hashedPassword;
    }
    // ----------------- 方法 -----------------
	public void addReservation(Reservation reservation){
		historyReservations.add(reservation);
	}
    public void handleViolationTimes(){
        violationTimes += 1;
        if (violationTimes >= 3 && suspensionEndDate == null){
            // 設定停權解除日期為現在的三個月後
            LocalDate endDate = LocalDate.now().plusMonths(3);
            setSuspensionEndDate(endDate); 
            System.out.println("用戶達到停權標準，停權解除日期: " + endDate);
        }
    }
    // ----------------- 服務方法 -----------------
    public String getStudentId(){
        return studentId;
    }
    public String getHashedPassword(){
        return hashedPassword;
    }
    public int getViolationTimes(){
        return violationTimes;
    }
    public List<Reservation> getActiveReservations(){
        List<Reservation> active = new ArrayList<>();
        for (Reservation r : historyReservations){
            if (r.getStatus() == ReservationStatus.RESERVED){
                active.add(r);
            }
        }
        return active;
    }
    public LocalDate getSuspensionEndDate(){
        return suspensionEndDate;
    }
    public void setSuspensionEndDate(LocalDate suspensionEndDate){
        this.suspensionEndDate = suspensionEndDate;
    }
    public void resetSuspension(){
        this.violationTimes = 0;
        this.suspensionEndDate = null;
    }
}

/*============================ Route 類別 ================================= */
class Route{
    private final String routeId;
    private final String routeName;
	// ----------------- 建構式 -----------------
    public Route(String routeId, String routeName){
        this.routeId = routeId;
        this.routeName = routeName;
    }
    // ----------------- 服務方法 -----------------
    public String getRouteId(){
        return routeId;
    }
    public String getRouteName(){
        return routeName;
    }
}

/*============================ Trip 類別 ================================= */
class Trip{
    private final String tripId;
    private final Route route;
    private final LocalDate date;
    private final LocalTime departureTime;
    private final int totalSeats = 20;
    private final List<Seat> seats = new ArrayList<>();
    private final Map<String, Seat> seatLookup = new HashMap<>();
    // ----------------- 建構式 -----------------
    public Trip(String tripId, Route route, LocalDate date, LocalTime departureTime){
        this.tripId = tripId;
        this.route = route;
        this.date = date;
        this.departureTime = departureTime;
        // 初始化該 Trip 的 20 個 Seat
        for (int i = 1; i <= totalSeats; i++){
            Seat newSeat = new Seat(i, this); // 傳入當前的 Trip
            this.seats.add(newSeat);
            this.seatLookup.put(String.valueOf(newSeat.getSeatNumber()), newSeat);
        }
    }
    // ----------------- 方法 -----------------
    public int getAvailableSeats(){
        int availableCount = 0;
        for (Seat seat : seats){
            if (seat.isAvailable()){
                availableCount += 1;
            }
        }
        return availableCount;
    }
    public Set<String> getOccupiedSeats(){
        Set<String> occupied = new HashSet<>();
        for (Seat seat : seats){
            if (!seat.isAvailable()) {
                occupied.add(String.valueOf(seat.getSeatNumber()));
            }
        }
        return occupied;
    }
    public Seat findSeatByNumber(String seatNumber){
        return seatLookup.get(seatNumber);
    }
    // ----------------- 服務方法 -----------------
    public String getTripId(){
        return tripId;
    }
    public Route getRoute(){
        return route;
    }
    public LocalDate getDate(){
        return date;
    }
    public LocalTime getDepartureTime(){
        return departureTime;
    }
    public LocalDateTime getDepartureDateTime(){
        return LocalDateTime.of(date, departureTime);
    }
    public int  getTotalSeats(){
        return totalSeats;
    }
}

/*============================ Seat 類別 ================================= */
class Seat{
    private final int seatNumber;
    private final Trip trip;
    private Reservation reservation = null; // 當前占用(預約)該 Seat 的 Reservation
    // ----------------- 建構式 -----------------
    public Seat(int seatNumber, Trip trip){
        this.seatNumber = seatNumber;
        this.trip = trip;
    }
    // ----------------- 方法 -----------------
	public boolean isAvailable(){
		return(this.reservation == null);
	}
    public void setReservation(Reservation reservation){
        this.reservation = reservation;
    }
    /// ----------------- 服務方法 -----------------
    public int getSeatNumber(){
        return seatNumber;
    }
    public Trip getTrip(){
        return trip;
    }
    public Reservation getReservation(){
        return reservation;
    }
}

/*============================ Reservation 類別 ================================= */
class Reservation{
    private final String reservationId;
    private final LocalDateTime reservationDateTime;
    private final Member member;
    private final Seat seat;
    private final Trip trip;
    private ReservationStatus status;
    // ----------------- 建構式 -----------------
    public Reservation(String reservationId, LocalDateTime reservationDateTime, Member member, Seat seat, Trip trip){
        this.reservationId = reservationId;
        this.reservationDateTime = reservationDateTime;
        this.member = member;
        this.seat = seat;
        this.trip = trip;
        this.status = ReservationStatus.RESERVED; // 新創的預約狀態預設為 CONFIRMED

        seat.setReservation(this); // 確保建立 Reservation 時，立即在對應的 Seat 設置參考
    }
    // ----------------- 方法 -----------------
    public void setStatus(ReservationStatus status){
        this.status = status;
    }
    public String getSeatNumber(){
        return String.valueOf(this.seat.getSeatNumber());
    }
    // ----------------- 服務方法 -----------------
    public String getReservationId(){
        return reservationId;
    }
    public LocalDateTime getReservationDateTime(){
        return reservationDateTime;
    }
    public Member getMember(){
        return member;
    }
    public Seat getSeat(){
        return seat;
    }
    public Trip getTrip(){
        return trip;
    }
    public ReservationStatus getStatus(){
        return status;
    }
}

/*============================ ReservationManager 類別 ================================= */
class ReservationManager{
    private static final int MAX_VIOLATION_TIMES = 3; // 定義違規次數上限
    private static final int CANCELLATION_GRACE_PERIOD_MINUTES = 30; // 定義最晚取消時間(發車前30分鐘)
    private long reservationIdCounter = 1; // 用於創建 Reservation 時所需的 id
    private final List<Trip> allTrips;
    private final Map<String, Member> members; 
    private final Map<String, Route> routes;

    // ----------------- 建構式 -----------------
    public ReservationManager(DataStore dataStore){
        this.allTrips = dataStore.getAllTrips();
        this.members = dataStore.getMembers();
        this.routes = dataStore.getRoutes();
    }
    // ----------------- 預約座位使用案例的相關方法 -----------------
    // 1.檢查 Member 是否可預約
    private boolean canReserve(Member member){
        // 檢查停權結束日期，並執行自動解鎖
        if (member.getSuspensionEndDate() != null){
            LocalDate today = LocalDate.now();
            if (today.isAfter(member.getSuspensionEndDate())){
                member.resetSuspension(); 
                System.out.println("用戶 " + member.getStudentId() + " 停權期滿，已自動解除停權。");
            }
        }
        // 進行停權檢查
        if (member.getViolationTimes() >= MAX_VIOLATION_TIMES){
            System.out.println("用戶 " + member.getStudentId() + " 違規次數過多，停權。");
            return false;
        }
        return true;
    }
    // 2.取得符合查詢條件的 Trip
    public List<Trip> getFilteredTrips(LocalDate date, LocalTime startTime, String routeName){
        List<Trip> filtered = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(30);
        // 檢查是否在預約日期範圍內
        if (date.isBefore(today) || date.isAfter(maxDate)){
            return filtered;
        }
        if(routeName == null || routeName.isEmpty()){
            System.out.println("請選擇路線。");
            return filtered;
        }
        // 將符合條件的 Trip 加入
        for (Trip t : allTrips){
            boolean matchesDateAndRoute = t.getDate().equals(date) && t.getRoute().getRouteName().equals(routeName);
            if(matchesDateAndRoute){
                boolean matchesTime = (startTime == null || t.getDepartureTime().compareTo(startTime) >= 0);
                boolean isFuture = t.getDepartureDateTime().isAfter(LocalDateTime.now());
                if (matchesTime && isFuture){
                    filtered.add(t);
                }
            }
        }
        return filtered;
    }
    public Trip findTripById(String id){
        for (Trip t : allTrips) {
            if (t.getTripId().equals(id)){
                return t;
            }
        }
        return null;
    }
    // 3.建立預約
    public Reservation createReservation(String studentId, String tripIdStr, String seatNumber){
        Member member = members.get(studentId);
        Trip trip = findTripById(tripIdStr);

        if (member == null || trip == null){
            return null;
        }

        Seat seat = trip.findSeatByNumber(seatNumber);
        //檢查 Member 是否可預約
        if (!canReserve(member)){
            return null;
        }
        if (seat == null || !seat.isAvailable()){
            System.out.println("座位已被預約或不存在，無法創建預約。");
            return null;
        }
        if (trip.getDepartureDateTime().isBefore(LocalDateTime.now())){
            return null;
        }
        // 實際創建預約
        String newId = String.valueOf(reservationIdCounter++); 
        Reservation newReservation = new Reservation(newId, LocalDateTime.now(), member, seat, trip); // 建立新預約
        member.addReservation(newReservation); // 將新預約加入 Member 的預約列表
        System.out.println("創建預約成功 " + newReservation.getReservationId());

        return newReservation;
    }
    // 4.取得 Member 有效的預約列表
    public List<Reservation> getMemberReservations(String studentId){
        Member member = members.get(studentId);
        if (member == null){
            return new ArrayList<>();   
        }
        return member.getActiveReservations();
    }

    // ----------------- 其他方法 -----------------
    // 簡化登入
    public Member findOrCreateMember(String studentId){
        return members.computeIfAbsent(studentId, k -> new Member(k, "default_pass")); 
    }
    // 取消預約
    public boolean cancelReservation(String reservationId, String studentId){
        Member member = members.get(studentId);
        String reservationIdStr = String.valueOf(reservationId); 
        Reservation reservationToCancel = null;

        if(member == null){
            return false;
        }
        for(Reservation r : member.getActiveReservations()){
            if (r.getReservationId().equals(reservationIdStr)){
                reservationToCancel = r;
                break;
            }
        }
        if(reservationToCancel == null){
            return false;
        }
        
        Trip trip = reservationToCancel.getTrip();
        Seat seat = reservationToCancel.getSeat(); 

        // 取消時間檢查 (決定是否計入違規)
        boolean isCancellationTimely = LocalDateTime.now().isBefore(trip.getDepartureDateTime().minus(CANCELLATION_GRACE_PERIOD_MINUTES, ChronoUnit.MINUTES));
        if (!isCancellationTimely){
            member.handleViolationTimes();
            System.out.println("警告：逾時取消，違規次數增加為 " + member.getViolationTimes());
            reservationToCancel.setStatus(ReservationStatus.LATEWITHDRAW);
        }else{
            reservationToCancel.setStatus(ReservationStatus.CANCELLED);
        }
        // 釋放座位
        seat.setReservation(null);
        return true;
    }
}

/*============================ DataStore 類別 ================================= */
class DataStore {
    private final List<Trip> allTrips; // 儲存所有 Trip
    private final Map<String, Member> members; // 儲存所有 Member
    private final Map<String, Route> routes; // 儲存所有 Route

    public DataStore(List<Trip> allTrips, Map<String, Member> members, Map<String, Route> routes) {
        this.allTrips = allTrips;
        this.members = members;
        this.routes = routes;
    }

    public List<Trip> getAllTrips() { return allTrips; }
    public Map<String, Member> getMembers() { return members; }
    public Map<String, Route> getRoutes() { return routes; }
}

/*============================ 主程式 ================================= */
public class SimpleApp { 
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void main(String[] args) throws IOException {
        // 初始化所有系統資料
        DataStore initialData = initializeSystemData();
        
        // 用初始化好的資料創建 Service
        final ReservationManager service = new ReservationManager(initialData);
        
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/api/login", new LoginHandler(service));
        server.createContext("/api/schedules", new ScheduleHandler(service));
        server.createContext("/api/bookings", new ReservationHandler(service));
        server.createContext("/", new StaticFileHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("Server started at http://localhost:8080");
    }

    // 資料生成方法
    private static DataStore initializeSystemData() {
        // Member 初始化
        Map<String, Member> members = new HashMap<>();
        members.put("A001", new Member("A001", "password123")); 
        members.put("B11100001", new Member("B11100001", "pass"));
        // Route 初始化
        Map<String, Route> routes = new HashMap<>();
        Route route1 = new Route("R01", "中央大學-桃園高鐵站");
        Route route2 = new Route("R02", "桃園高鐵站-中央大學");
        Route route3 = new Route("R03", "中央大學-中壢火車站");
        Route route4 = new Route("R04", "中壢火車站-中央大學");
        
        routes.put(route1.getRouteName(), route1);
        routes.put(route2.getRouteName(), route2);
        routes.put(route3.getRouteName(), route3);
        routes.put(route4.getRouteName(), route4);
        
        List<Route> allRoutes = List.of(route1, route2, route3, route4);

        // Trip 初始化
        List<Trip> allTrips = new ArrayList<>();
        LocalTime[] times = {
            LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(11, 0),
            LocalTime.of(13, 30), LocalTime.of(15, 0), LocalTime.of(16, 30)
        };
        
        long tripIdCounter = 1; // ID 計數器
        LocalDate today = LocalDate.now();
        
        // 生成一個月的 Trip 資料
        for (int i = 0; i < 30; i++){
            LocalDate date = today.plusDays(i);
            for (Route r : allRoutes){
                for (LocalTime time : times){
                    String tripIdStr = String.valueOf(tripIdCounter++);
                    allTrips.add(new Trip(tripIdStr, r, date, time)); 
                }
            }
        }
        System.out.println("系統初始化：共生成 " + allTrips.size() + " 筆班次資料。");
        
        return new DataStore(allTrips, members, routes);
    }

    /*============================ Handler 1: Login ================================= */
    static class LoginHandler implements HttpHandler {
        private final ReservationManager service;
        
        public LoginHandler(ReservationManager service) { this.service = service; }
        
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = getRequestBody(exchange);
                String username = extractJsonValue(body, "username"); 
                
                if (username != null && !username.isEmpty()) {
                    service.findOrCreateMember(username); 
                    
                    String response = "{\"success\": true, \"studentId\": \"" + username + "\"}";
                    sendResponse(exchange, 200, response);
                } else {
                    String response = "{\"success\": false, \"message\": \"登入失敗\"}";
                    sendResponse(exchange, 400, response);
                }
            }
        }
    }
    
    /*============================ Handler 2: Schedules ================================= */
    static class ScheduleHandler implements HttpHandler {
        private final ReservationManager service;
        
        public ScheduleHandler(ReservationManager service) { this.service = service; }
        
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                String dateStr = params.get("date");
                String timeStr = params.get("time");
                String routeName = params.get("route"); 
                LocalDate queryDate;
                LocalTime queryTime = null;

                try {
                    queryDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                    if (timeStr != null && !timeStr.isEmpty()) {
                        queryTime = LocalTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_TIME);
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "{\"error\": \"日期或時間格式錯誤\"}");
                    return;
                }
                
                List<Trip> filtered = service.getFilteredTrips(queryDate, queryTime, routeName);
                
                if (filtered.isEmpty() && queryDate.isAfter(LocalDate.now().plusDays(30))) {
                    sendResponse(exchange, 400, "{\"error\": \"尚未開放預約\"}");
                    return;
                }
                
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < filtered.size(); i++) {
                    if (i > 0) json.append(",");
                    Trip t = filtered.get(i);
                    
                    StringBuilder occupiedSeats = new StringBuilder("[");
                    boolean first = true;
                    for (String seatNum : t.getOccupiedSeats()) {
                        if (!first) occupiedSeats.append(",");
                        occupiedSeats.append("\"").append(seatNum).append("\"");
                        first = false;
                    }
                    occupiedSeats.append("]");
                    
                    json.append("{")
                        .append("\"id\":").append("\"").append(t.getTripId()).append("\",")
                        .append("\"date\":\"").append(t.getDate()).append("\",")
                        .append("\"route\":\"").append(t.getRoute().getRouteName()).append("\",") 
                        .append("\"departureTime\":\"").append(t.getDepartureTime()).append("\",")
                        .append("\"totalSeats\":").append(t.getTotalSeats()).append(",")
                        .append("\"availableSeats\":").append(t.getAvailableSeats()).append(",") 
                        .append("\"occupiedSeats\":").append(occupiedSeats.toString())
                        .append("}");
                }
                json.append("]");
                
                sendResponse(exchange, 200, json.toString());
            }
        }
    }

    /*============================ Handler 3: Reservations ================================= */
    static class ReservationHandler implements HttpHandler {
        private final ReservationManager service;
        
        public ReservationHandler(ReservationManager service) { this.service = service; }
        
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            String path = exchange.getRequestURI().getPath();
            
            if ("GET".equals(exchange.getRequestMethod())) {
                String studentId = path.substring(path.lastIndexOf("/") + 1);
                
                List<Reservation> userReservations = service.getMemberReservations(studentId);
                
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < userReservations.size(); i++) {
                    if (i > 0) json.append(",");
                    Reservation r = userReservations.get(i);
                    Trip t = r.getTrip();
                    
                    String statusDisplay;
                    long daysUntilTrip = ChronoUnit.DAYS.between(LocalDate.now(), t.getDate());
                    
                    if (daysUntilTrip <= 3 && daysUntilTrip >= 0) {
                        statusDisplay = "即將到來";
                    } else if (daysUntilTrip > 3) {
                        statusDisplay = daysUntilTrip + "天後";
                    } else {
                        statusDisplay = "已過期"; 
                    }
                    
                    json.append("{")
                        .append("\"id\":").append("\"").append(r.getReservationId()).append("\",") 
                        .append("\"studentId\":\"").append(r.getMember().getStudentId()).append("\",")
                        .append("\"seatNumber\":\"").append(r.getSeatNumber()).append("\",")
                        .append("\"statusDisplay\":\"").append(statusDisplay).append("\",") 
                        .append("\"schedule\":{")
                        .append("\"id\":").append("\"").append(t.getTripId()).append("\",")
                        .append("\"date\":\"").append(t.getDate()).append("\",")
                        .append("\"route\":\"").append(t.getRoute().getRouteName()).append("\",") 
                        .append("\"departureTime\":\"").append(t.getDepartureTime()).append("\"")
                        .append("}")
                        .append("}");
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString());

            } else if ("POST".equals(exchange.getRequestMethod())) {
                String body = getRequestBody(exchange);
                String studentId = extractJsonValue(body, "studentId");
                String scheduleIdStr = extractJsonValue(body, "scheduleId");
                String seatNumber = extractJsonValue(body, "seatNumber");
                
                if (studentId != null && scheduleIdStr != null && seatNumber != null) {
                    // 建立預約
                    Reservation newReservation = service.createReservation(studentId, scheduleIdStr, seatNumber);
                    
                    if (newReservation != null) {
                        String response = "{\"id\": \"" + newReservation.getReservationId() + "\", \"success\": true}"; 
                        sendResponse(exchange, 200, response);
                        return;
                    }
                }
                sendResponse(exchange, 400, "{\"error\": \"預約失敗，可能原因：座位已被預約或違規停權\"}");
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                String reservationIdStr = path.substring(path.lastIndexOf("/") + 1);
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                String studentId = params.get("studentId");
                
                if (reservationIdStr != null && studentId != null) {
                    // 取消預約
                    if (service.cancelReservation(reservationIdStr, studentId)) {
                        sendResponse(exchange, 200, "{\"success\": true}");
                        return;
                    }
                }
                sendResponse(exchange, 400, "{\"error\": \"取消失敗或預約不存在\"}");
            }
        }
    }
    
    /*============================ Handler 4: Static Files ================================= */
    static class StaticFileHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
             String path = exchange.getRequestURI().getPath();
             if (path.equals("/")) path = "/index.html";
             
             File file = new File("." + path);
             if (file.exists() && file.isFile()) {
                 String contentType = getContentType(path);
                 exchange.getResponseHeaders().set("Content-Type", contentType);
                 exchange.sendResponseHeaders(200, file.length());
                 
                 FileInputStream fis = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody();
                 byte[] buffer = new byte[1024];
                 int bytesRead;
                 while ((bytesRead = fis.read(buffer)) != -1) {
                     os.write(buffer, 0, bytesRead);
                 }
                 fis.close();
                 os.close();
             } else {
                 String response = "404 Not Found";
                 sendResponse(exchange, 404, response);
             }
         }
    }
    
    /*============================ UTILITY METHODS (輔助方法) ================================= */
    private static void setCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.getBytes("UTF-8").length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes("UTF-8"));
        }
    }
    
    private static String getRequestBody(HttpExchange exchange) throws IOException {
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            return requestBody.toString();
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null) {
             String[] pairs = query.split("&");
             for (String pair : pairs) {
                 String[] keyValue = pair.split("=");
                 if (keyValue.length == 2) {
                     try {
                         result.put(keyValue[0], URLDecoder.decode(keyValue[1], "UTF-8"));
                     } catch (UnsupportedEncodingException e) {
                         result.put(keyValue[0], keyValue[1]);
                     }
                 }
             }
        }
        return result;
    }
    
    private static String extractJsonValue(String json, String key) {
         String searchKey = "\"" + key + "\":";
         int startIndex = json.indexOf(searchKey);
         if (startIndex == -1) return null;
         
         startIndex += searchKey.length();
         while (startIndex < json.length() && (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '\t')) {
             startIndex++;
         }
         
         if (startIndex >= json.length()) return null;
         
         char firstChar = json.charAt(startIndex);
         if (firstChar == '"') {
             startIndex++;
             int endIndex = json.indexOf('"', startIndex);
             if (endIndex == -1) return null;
             return json.substring(startIndex, endIndex);
         } else {
             int endIndex = startIndex;
             while (endIndex < json.length() && (Character.isDigit(json.charAt(endIndex)) || json.charAt(endIndex) == '.')) {
                 endIndex++;
             }
             if (endIndex == startIndex) return null;
             return json.substring(startIndex, endIndex);
         }
    }

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "text/plain";
    }
}