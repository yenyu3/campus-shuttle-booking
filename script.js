// API 基礎URL
const API_BASE = 'http://localhost:8080/api';

// 用戶和預約資料
let currentUser = null;
let myBookings = [];
let selectedSeat = null;
let currentBookingInfo = null;

// DOM 元素
const loginPage = document.getElementById('loginPage');
const mainPage = document.getElementById('mainPage');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const loginBtn = document.getElementById('loginBtn');
const logoutBtn = document.getElementById('logoutBtn');
const userDisplay = document.getElementById('userDisplay');
const dateInput = document.getElementById('date');
const routeSelect = document.getElementById('route');
const searchBtn = document.getElementById('searchBtn');
const scheduleList = document.getElementById('scheduleList');
const myBookingsDiv = document.getElementById('myBookings');
const seatModal = document.getElementById('seatModal');
const modalTitle = document.getElementById('modalTitle');
const confirmBooking = document.getElementById('confirmBooking');
const cancelBooking = document.getElementById('cancelBooking');
const closeModal = document.querySelector('.close');

// 初始化事件監聽
function initializeEventListeners() {
    if (loginBtn) loginBtn.addEventListener('click', login);
    if (logoutBtn) logoutBtn.addEventListener('click', logout);
    if (closeModal) closeModal.addEventListener('click', closeSeatModal);
    if (cancelBooking) cancelBooking.addEventListener('click', closeSeatModal);
    if (confirmBooking) confirmBooking.addEventListener('click', confirmSeatBooking);
    if (searchBtn) searchBtn.addEventListener('click', searchSchedule);
}

// DOM載入完成後初始化
document.addEventListener('DOMContentLoaded', initializeEventListeners);

// 登入功能
async function login() {
    const username = usernameInput.value.trim();
    const password = passwordInput.value.trim();
    
    if (!username || !password) {
        alert('請輸入學號和密碼');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ username, password })
        });
        
        const result = await response.json();
        
        if (result.success) {
            currentUser = result.studentId;
            myBookings = []; // 清空前一個用戶的預約記錄
            userDisplay.textContent = `歡迎，${username}`;
            
            // 重置日期和路線預設值
            if (dateInput) dateInput.valueAsDate = new Date();
            if (routeSelect) routeSelect.value = '';
            
            loginPage.style.display = 'none';
            mainPage.style.display = 'block';
            loadMyBookings();
        } else {
            alert('登入失敗');
        }
    } catch (error) {
        console.error('登入錯誤:', error);
        alert('登入失敗，請檢查網路連線');
    }
}

// 登出功能
function logout() {
    currentUser = null;
    myBookings = []; // 清空預約記錄
    usernameInput.value = '';
    passwordInput.value = '';
    scheduleList.innerHTML = ''; // 清空班次列表
    
    // 重置日期和路線
    if (dateInput) dateInput.value = '';
    if (routeSelect) routeSelect.value = '';
    
    loginPage.style.display = 'flex';
    mainPage.style.display = 'none';
    displayMyBookings(); // 更新顯示
}

// 查詢班次功能
async function searchSchedule() {
    const selectedDate = dateInput.value;
    const selectedRoute = routeSelect.value;
    
    if (!selectedDate) {
        alert('請選擇日期');
        return;
    }
    
    try {
        let url = `${API_BASE}/schedules?date=${selectedDate}`;
        if (selectedRoute) {
            url += `&route=${encodeURIComponent(selectedRoute)}`;
        }
        
        const response = await fetch(url);
        
        if (!response.ok) {
            const errorData = await response.json();
            if (errorData.error) {
                scheduleList.innerHTML = `<div class="empty-message">${errorData.error}</div>`;
                return;
            }
        }
        
        const schedules = await response.json();
        displaySchedule(schedules, selectedDate);
    } catch (error) {
        console.error('查詢班次錯誤:', error);
        alert('查詢失敗，請檢查網路連線');
    }
}

// 顯示班次列表
function displaySchedule(schedules, date) {
    if (schedules.length === 0) {
        scheduleList.innerHTML = '<div class="empty-message">此日期暫無班次</div>';
        return;
    }
    
    scheduleList.innerHTML = schedules.map(schedule => {
        return `
            <div class="schedule-card">
                <div class="schedule-info">
                    <div class="time">${schedule.departureTime}</div>
                    <div class="route">${schedule.route}</div>
                    <div class="seats ${schedule.availableSeats <= 5 ? 'low' : ''}">
                        剩餘座位：${schedule.availableSeats} 位
                    </div>
                </div>
                <button class="btn-secondary" 
                        onclick="openSeatSelection(${schedule.id}, '${schedule.route}', '${schedule.departureTime}', '${date}')"
                        ${schedule.availableSeats === 0 ? 'disabled' : ''}>
                    ${schedule.availableSeats === 0 ? '已滿' : '選位預約'}
                </button>
            </div>
        `;
    }).join('');
}

// 開啟座位選擇
function openSeatSelection(scheduleId, route, time, date) {
    currentBookingInfo = { scheduleId, route, time, date };
    modalTitle.textContent = `${route} - ${time}`;
    
    generateSeatMap();
    seatModal.style.display = 'block';
}

// 生成座位圖
function generateSeatMap() {
    const colLeft = document.getElementById('colLeft');
    const colRight = document.getElementById('colRight');
    colLeft.innerHTML = '';
    colRight.innerHTML = '';
    selectedSeat = null;
    confirmBooking.disabled = true;
    
    const leftOrder = [1, 3, 5, 7, 9, 11, 13, 15, 17, 19];
    const rightOrder = [2, 4, 6, 8, 10, 12, 14, 16, 18, 20];
    
    function createSeat(num) {
        const seat = document.createElement('div');
        seat.className = 'seat available';
        seat.dataset.num = num;
        seat.innerHTML = `<div class="seat-number">${num}</div>`;
        seat.addEventListener('click', () => selectSeat(num, seat));
        return seat;
    }
    
    leftOrder.forEach(num => {
        colLeft.appendChild(createSeat(num));
    });
    
    rightOrder.forEach(num => {
        colRight.appendChild(createSeat(num));
    });
}

// 選擇座位
function selectSeat(seatNumber, seatElement) {
    // 清除之前選擇
    document.querySelectorAll('.seat.selected').forEach(s => {
        s.classList.remove('selected');
        s.classList.add('available');
    });
    
    // 選擇新座位
    seatElement.classList.remove('available');
    seatElement.classList.add('selected');
    selectedSeat = seatNumber;
    confirmBooking.disabled = false;
}

// 確認預約
async function confirmSeatBooking() {
    if (!selectedSeat || !currentBookingInfo) return;
    
    try {
        const response = await fetch(`${API_BASE}/bookings`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                studentId: currentUser,
                scheduleId: currentBookingInfo.scheduleId,
                seatNumber: selectedSeat.toString()
            })
        });
        
        const result = await response.json();
        
        if (result.id) {
            closeSeatModal();
            showSuccessMessage(`預約成功！座位 ${selectedSeat}`);
            loadMyBookings();
            // 重新查詢班次以更新可用座位
            searchSchedule();
        } else {
            alert('預約失敗：' + (result.error || '未知錯誤'));
        }
    } catch (error) {
        console.error('預約錯誤:', error);
        alert('預約失敗，請檢查網路連線');
    }
}

// 關閉座位選擇彈窗
function closeSeatModal() {
    seatModal.style.display = 'none';
    selectedSeat = null;
    currentBookingInfo = null;
}

// 載入我的預約
async function loadMyBookings() {
    if (!currentUser) return;
    
    try {
        const response = await fetch(`${API_BASE}/bookings/${currentUser}`);
        myBookings = await response.json();
        displayMyBookings();
    } catch (error) {
        console.error('載入預約錯誤:', error);
    }
}

// 顯示我的預約
function displayMyBookings() {
    const bookingCount = document.getElementById('bookingCount');
    bookingCount.textContent = myBookings.length;
    
    if (myBookings.length === 0) {
        myBookingsDiv.innerHTML = '<div class="empty-message">🚌 尚無預約記錄，趕快預約您的接駁車吧！</div>';
        return;
    }
    
    myBookingsDiv.innerHTML = myBookings.map(booking => {
        const today = new Date();
        const bookingDate = new Date(booking.schedule.date);
        
        // 計算日期差異（只考慮日期，不考慮時間）
        const todayDateOnly = new Date(today.getFullYear(), today.getMonth(), today.getDate());
        const bookingDateOnly = new Date(bookingDate.getFullYear(), bookingDate.getMonth(), bookingDate.getDate());
        const diffTime = bookingDateOnly - todayDateOnly;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        
        let statusText, statusClass;
        if (diffDays <= 3) {
            statusText = '即將到來';
            statusClass = 'status-upcoming';
        } else {
            statusText = `${diffDays}天後`;
            statusClass = 'status-future';
        }
        
        return `
            <div class="booking-card">
                <div class="booking-status ${statusClass}">${statusText}</div>
                <div class="schedule-info">
                    <div class="time">${booking.schedule.departureTime}</div>
                    <div class="route">${booking.schedule.route}</div>
                    <div class="seats">日期：${booking.schedule.date} | 座位：${booking.seatNumber}</div>
                </div>
                <button class="delete-btn" onclick="deleteBooking(${booking.id})">
                    刪除
                </button>
            </div>
        `;
    }).join('');
}

// 刪除預約
async function deleteBooking(bookingId) {
    if (!confirm('確定要刪除這個預約嗎？')) return;
    
    try {
        const response = await fetch(`${API_BASE}/bookings/${bookingId}?studentId=${currentUser}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        
        if (result.success) {
            showSuccessMessage('預約已刪除');
            loadMyBookings();
            searchSchedule(); // 重新查詢以更新座位
        } else {
            alert('刪除失敗：' + (result.error || '未知錯誤'));
        }
    } catch (error) {
        console.error('刪除預約錯誤:', error);
        alert('刪除失敗，請檢查網路連線');
    }
}

// 顯示成功訊息
function showSuccessMessage(message) {
    const messageDiv = document.createElement('div');
    messageDiv.textContent = message;
    messageDiv.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #27ae60;
        color: white;
        padding: 15px 20px;
        border-radius: 5px;
        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
        z-index: 1000;
        font-weight: bold;
    `;
    
    document.body.appendChild(messageDiv);
    
    setTimeout(() => {
        if (document.body.contains(messageDiv)) {
            document.body.removeChild(messageDiv);
        }
    }, 3000);
}

// 快捷操作功能
function setToday() {
    if (dateInput) {
        dateInput.valueAsDate = new Date();
    }
}

function setTomorrow() {
    if (dateInput) {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        dateInput.valueAsDate = tomorrow;
    }
}

function setRoute(route) {
    if (routeSelect) {
        routeSelect.value = route;
    }
}

// 點擊彈窗外部關閉
window.addEventListener('click', (event) => {
    if (event.target === seatModal) {
        closeSeatModal();
    }
});