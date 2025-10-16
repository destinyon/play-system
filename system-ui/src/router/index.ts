import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    {
      path: '/app',
      component: MainLayout,
      children: [
        // Admin section
        { path: 'admin/dashboard', name: 'admin-dashboard', component: () => import('../views/placeholder/AdminDashboard.vue') },
        
        // Guest/User section
        { path: 'guest/home', name: 'guest-home', component: () => import('../views/GuestHome.vue') },
        { path: 'guest/cart', name: 'guest-cart', component: () => import('../views/placeholder/GuestCart.vue') },
        { path: 'guest/orders', name: 'guest-orders', component: () => import('../views/placeholder/GuestOrders.vue') },
        { path: 'guest/chat', name: 'guest-chat', component: () => import('../views/placeholder/Chat.vue') },

        { path: 'chat', redirect: '/app/guest/chat' },

        // Shared
        { path: 'profile', name: 'profile', component: () => import('../views/placeholder/Profile.vue') },

        // Restaurateur section  
        { path: 'restaurateur/home', name: 'restaurateur-home', component: () => import('../views/RestaurateurHome.vue') },
        { path: 'restaurateur/dashboard', name: 'restaurateur-dashboard', component: () => import('../views/placeholder/RestaurateurDashboard.vue') },
        { path: 'restaurateur/orders', name: 'restaurateur-orders', component: () => import('../views/placeholder/RestaurateurOrders.vue') },
        { path: 'restaurateur/dishes', name: 'restaurateur-dishes', component: () => import('../views/placeholder/RestaurateurDishes.vue') },
        { path: 'restaurateur/chat', name: 'restaurateur-chat', component: () => import('../views/placeholder/Chat.vue') },
        { path: 'restaurateur/restaurant', name: 'restaurateur-restaurant', component: () => import('../views/placeholder/RestaurateurRestaurant.vue') },

        // Deliveryman section
        { path: 'deliveryman/home', name: 'deliveryman-home', component: () => import('../views/DeliverymanHome.vue') },
        { path: 'deliveryman/dashboard', name: 'deliveryman-dashboard', component: () => import('../views/placeholder/DeliverymanDashboard.vue') },
        { path: 'deliveryman/deliver', name: 'deliveryman-deliver', component: () => import('../views/placeholder/DeliverymanDeliver.vue') },
        { path: 'deliveryman/chat', name: 'deliveryman-chat', component: () => import('../views/placeholder/Chat.vue') },
        { path: 'deliveryman/profile', name: 'deliveryman-profile', component: () => import('../views/placeholder/Profile.vue') },

        // Backward compatibility for legacy rider paths
        { path: 'rider/dashboard', redirect: '/app/deliveryman/dashboard' },
        { path: 'rider/deliver', redirect: '/app/deliveryman/deliver' },
        { path: 'rider/chat', redirect: '/app/deliveryman/chat' },
        { path: 'rider/profile', redirect: '/app/deliveryman/profile' },

        // Legacy
        { path: 'markers', name: 'markers', component: () => import('../views/MarkersView.vue') },
        { path: 'routes', name: 'routes', component: () => import('../views/RoutesView.vue') },
        { path: 'settings', name: 'settings', component: () => import('../views/AboutView.vue') },
      ],
    },
    { path: '/gdmap', redirect: '/app/map' },
  ],
})

export default router
