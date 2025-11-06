// WebSocket Notification System
class NotificationWebSocket {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 5000;
    }

    // Initialize WebSocket connection
    connect() {
        const authToken = localStorage.getItem('authToken');
        if (!authToken) {
            console.log('No auth token found, skipping WebSocket connection');
            return;
        }

        try {
            // Create SockJS connection
            const socket = new SockJS('/ws');
            
            // Use the global Stomp object from the CDN
            this.stompClient = window.StompJs ? 
                new window.StompJs.Client({
                    webSocketFactory: () => socket,
                    debug: (str) => console.log('STOMP Debug:', str),
                    reconnectDelay: 5000,
                    heartbeatIncoming: 4000,
                    heartbeatOutgoing: 4000,
                }) : 
                window.Stomp.over(socket);

            if (window.StompJs) {
                // New STOMP.js API
                this.stompClient.onConnect = (frame) => this.onConnected(frame);
                this.stompClient.onStompError = (error) => this.onError(error);
                this.stompClient.onWebSocketClose = () => this.onError('WebSocket closed');
                this.stompClient.activate();
            } else {
                // Legacy STOMP API
                this.stompClient.debug = (str) => console.log('STOMP Debug:', str);
                this.stompClient.connect({}, 
                    (frame) => this.onConnected(frame),
                    (error) => this.onError(error)
                );
            }

        } catch (error) {
            console.error('Error initializing WebSocket:', error);
            this.scheduleReconnect();
        }
    }

    // Handle successful connection
    onConnected(frame) {
        console.log('Connected to WebSocket:', frame);
        this.connected = true;
        this.reconnectAttempts = 0;

        const currentUser = JSON.parse(localStorage.getItem('userData'));
        if (!currentUser || !currentUser.username) {
            console.error('No user data found');
            return;
        }

        try {
            // Subscribe to user-specific notifications
            this.stompClient.subscribe(`/user/queue/notifications`, (message) => {
                const notification = JSON.parse(message.body);
                this.handleNewNotification(notification);
            });

            // Subscribe to unread count updates
            this.stompClient.subscribe(`/user/queue/unread-count`, (message) => {
                const unreadCount = JSON.parse(message.body);
                this.updateNotificationBadge(unreadCount);
            });

            // Subscribe to ride status updates
            this.stompClient.subscribe(`/user/queue/ride-status-updates`, (message) => {
                const statusUpdate = JSON.parse(message.body);
                this.handleRideStatusUpdate(statusUpdate);
            });

            // Send connection message
            if (window.StompJs && this.stompClient.publish) {
                this.stompClient.publish({
                    destination: '/app/notifications.connect',
                    body: currentUser.username
                });
            } else {
                this.stompClient.send('/app/notifications.connect', {}, currentUser.username);
            }

            console.log('WebSocket subscriptions established');

        } catch (error) {
            console.error('Error setting up subscriptions:', error);
        }
    }

    // Handle connection error
    onError(error) {
        console.error('WebSocket connection error:', error);
        this.connected = false;
        this.scheduleReconnect();
    }

    // Schedule reconnection
    scheduleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`Scheduling reconnect attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts} in ${this.reconnectDelay}ms`);
            
            setTimeout(() => {
                console.log('Attempting to reconnect...');
                this.connect();
            }, this.reconnectDelay);
        } else {
            console.error('Max reconnection attempts reached');
        }
    }

    // Handle new notification
    handleNewNotification(notification) {
        console.log('New notification received:', notification);
        
        // Show browser notification if permission granted
        if (Notification.permission === 'granted') {
            new Notification(notification.title, {
                body: notification.message,
                icon: '/favicon.ico',
                tag: 'ride-notification'
            });
        }
        
        // Show toast notification
        this.showNotificationToast(notification);
        
        // Refresh notifications if panel is open
        if (typeof loadNotifications === 'function') {
            loadNotifications();
        }
    }

    // Show notification toast
    showNotificationToast(notification) {
        // Create toast element
        const toast = document.createElement('div');
        toast.className = 'notification-toast';
        toast.innerHTML = `
            <div class="toast-header">
                <strong>${notification.title}</strong>
                <button onclick="this.parentElement.parentElement.remove()" class="toast-close">&times;</button>
            </div>
            <div class="toast-body">${notification.message}</div>
        `;
        
        // Add toast styles if not already added
        this.addToastStyles();
        
        // Add to page
        document.body.appendChild(toast);
        
        // Auto remove after 5 seconds
        setTimeout(() => {
            if (toast.parentElement) {
                toast.remove();
            }
        }, 5000);
    }

    // Add toast CSS styles
    addToastStyles() {
        if (document.getElementById('toast-styles')) return;

        const style = document.createElement('style');
        style.id = 'toast-styles';
        style.textContent = `
            .notification-toast {
                position: fixed;
                top: 100px;
                right: 20px;
                background: white;
                border-left: 4px solid #3EBB9E;
                border-radius: 8px;
                box-shadow: 0 4px 15px rgba(0,0,0,0.1);
                padding: 15px;
                max-width: 350px;
                z-index: 1000;
                animation: slideInRight 0.3s ease;
            }
            .toast-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 8px;
                color: #333;
                font-weight: 600;
            }
            .toast-close {
                background: none;
                border: none;
                font-size: 18px;
                cursor: pointer;
                color: #999;
            }
            .toast-close:hover {
                color: #333;
            }
            .toast-body {
                color: #666;
                font-size: 14px;
            }
            @keyframes slideInRight {
                from { transform: translateX(100%); opacity: 0; }
                to { transform: translateX(0); opacity: 1; }
            }
        `;
        document.head.appendChild(style);
    }

    // Update notification badge
    updateNotificationBadge(count) {
        const badge = document.querySelector('.notification-badge');
        if (badge) {
            if (count > 0) {
                badge.textContent = count > 99 ? '99+' : count;
                badge.style.display = 'block';
            } else {
                badge.style.display = 'none';
            }
        }
    }

    // Handle ride status updates
    handleRideStatusUpdate(statusUpdate) {
        console.log('Ride status update received:', statusUpdate);

        // Update ride status in UI if the ride is currently displayed
        this.updateRideStatusInUI(statusUpdate);

        // Show notification for status change
        const notification = {
            title: 'Ride Status Update',
            message: `Ride from ${statusUpdate.source} to ${statusUpdate.destination} is now ${statusUpdate.newStatus}`
        };
        this.showNotificationToast(notification);
    }

    // Update ride status in UI elements
    updateRideStatusInUI(statusUpdate) {
        // Find ride elements by rideId and update their status
        const rideElements = document.querySelectorAll(`[data-ride-id="${statusUpdate.rideId}"]`);
        rideElements.forEach(element => {
            const statusElement = element.querySelector('.ride-status, .status');
            if (statusElement) {
                statusElement.textContent = statusUpdate.newStatus;
                statusElement.className = `status status-${statusUpdate.newStatus.toLowerCase()}`;
            }
        });

        // Update available seats if applicable
        if (statusUpdate.newStatus === 'FULL') {
            rideElements.forEach(element => {
                const seatsElement = element.querySelector('.available-seats');
                if (seatsElement) {
                    seatsElement.textContent = '0';
                }
            });
        }
    }

    // Disconnect WebSocket
    disconnect() {
        if (this.stompClient && this.connected) {
            if (window.StompJs && this.stompClient.deactivate) {
                this.stompClient.deactivate();
            } else {
                this.stompClient.disconnect(() => {
                    console.log('WebSocket disconnected');
                });
            }
            this.connected = false;
        }
    }

    // Check if connected
    isConnected() {
        return this.connected;
    }
}

// Global WebSocket instance
let notificationWS = null;

// Initialize WebSocket when DOM is ready
function initializeWebSocket() {
    try {
        if (!notificationWS) {
            notificationWS = new NotificationWebSocket();
        }
        notificationWS.connect();
    } catch (error) {
        console.error('❌ Error initializing WebSocket:', error);
        // Don't let WebSocket errors break the page
    }
}

// Request notification permission
function requestNotificationPermission() {
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission().then(permission => {
            console.log('Notification permission:', permission);
        });
    }
}

// REMOVED: Auto-initialization to prevent race condition
// The dashboard will call initializeWebSocket() manually after authentication
// This prevents multiple DOMContentLoaded listeners from conflicting

// Disconnect when page unloads
window.addEventListener('beforeunload', () => {
    if (notificationWS) {
        notificationWS.disconnect();
    }
});