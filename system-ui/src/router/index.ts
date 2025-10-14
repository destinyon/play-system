import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: MainLayout,
      redirect: '/map',
      children: [
        {
          path: 'map',
          name: 'map',
          component: () => import('../views/GDMap.vue'),
        },
        {
          path: 'markers',
          name: 'markers',
          component: () => import('../views/MarkersView.vue'),
        },
        {
          path: 'routes',
          name: 'routes',
          component: () => import('../views/RoutesView.vue'),
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('../views/AboutView.vue'),
        },
      ],
    },
    // Legacy routes (can be removed later)
    {
      path: '/gdmap',
      redirect: '/map',
    },
  ],
})

export default router
