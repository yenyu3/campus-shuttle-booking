# 校園接駁車預約系統 🚌

一個基於Java後端和HTML/CSS/JavaScript前端的校園接駁車線上預約系統，提供學生便捷的交通預約服務。

## 📋 功能特色

### 🔐 用戶管理
- 簡易登入系統（任意學號密碼即可登入）
- 多用戶獨立預約記錄
- 安全的用戶會話管理

### 🚍 班次管理
- **4條主要路線**：
  - 中央大學 ↔ 桃園高鐵站
  - 中央大學 ↔ 中壢火車站
- **每日6個時段**：08:00, 09:30, 11:00, 13:30, 15:00, 16:30
- **30天預約期間**：支援從當天起30天內的預約
- 即時座位狀態更新

### 🪑 座位選擇
- 視覺化座位圖（20人座巴士佈局）
- 即時座位可用性顯示
- 防止重複預約同一座位

### 📅 預約管理
- 個人預約記錄查看
- 智慧狀態標示：
  - 3天內：「即將到來」
  - 超過3天：「X天後」
- 一鍵取消預約功能

### 🔍 查詢功能
- 依日期和路線篩選班次
- 快捷按鈕（今天、明天、常用路線）
- 即時可用座位數顯示

## 🛠️ 技術架構

### 後端
- **語言**：Java 8+
- **架構**：原生HTTP Server
- **資料庫**：記憶體資料庫（H2 in-memory）
- **API**：RESTful API設計

### 前端
- **語言**：HTML5, CSS3, JavaScript (ES6+)
- **樣式**：響應式設計
- **互動**：原生JavaScript，無框架依賴

## 🚀 快速開始

### 環境需求
- Java 8 或以上版本
- 支援的作業系統：Windows, macOS, Linux

### 安裝步驟

1. **克隆專案**
   ```bash
   git clone https://github.com/your-username/campus-shuttle-booking.git
   cd campus-shuttle-booking
   ```

2. **編譯後端**
   ```bash
   javac -encoding UTF-8 SimpleApp.java
   ```

3. **啟動服務**
   ```bash
   java SimpleApp
   ```

4. **訪問系統**
   - 開啟瀏覽器
   - 前往 `http://localhost:8080`
   - 使用任意學號和密碼登入

### 🖥️ 使用說明

#### 登入系統
1. 在登入頁面輸入任意學號（如：`A001`）和密碼
2. 點擊「登入」按鈕進入主頁面

#### 查詢班次
1. 選擇預約日期（預設為今天）
2. 選擇路線（4條路線可選）
3. 點擊「查詢」查看可用班次

#### 預約座位
1. 在班次列表中點擊「選位預約」
2. 在座位圖中選擇喜歡的座位
3. 點擊「確認預約」完成預約

#### 管理預約
- 在「我的預約」區域查看所有預約記錄
- 點擊「刪除」按鈕可取消預約
- 預約狀態會自動更新（即將到來/X天後）

## 📁 專案結構

```
campus-shuttle-booking/
├── SimpleApp.java          # 後端主程式
├── index.html             # 前端主頁面
├── script.js              # 前端JavaScript邏輯
├── style.css              # 前端樣式表
└── README.md              # 專案說明文件
```

## 🔧 API 文檔

### 登入
- **POST** `/api/login`
- Body: `{"username": "學號", "password": "密碼"}`

### 查詢班次
- **GET** `/api/schedules?date=YYYY-MM-DD&route=路線名稱`

### 查詢個人預約
- **GET** `/api/bookings/{studentId}`

### 建立預約
- **POST** `/api/bookings`
- Body: `{"studentId": "學號", "scheduleId": 班次ID, "seatNumber": "座位號"}`

### 取消預約
- **DELETE** `/api/bookings/{bookingId}?studentId=學號`
