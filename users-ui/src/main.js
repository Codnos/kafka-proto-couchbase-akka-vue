import Vue from 'vue'
import VueRouter from 'vue-router'
import App from './App.vue'
import Users from './Users.vue'

Vue.config.productionTip = false
Vue.use(VueRouter)

const Home = { template: '<div><h2>Home</h2></div>' }
const About = { template: '<div><h2>About</h2></div>' }


const routes = [
  { path: '/', component: Home },
  { path: '/about', component: About },
  { path: '/users', component: Users },
]

const router = new VueRouter({
  routes // short for `routes: routes`
})

new Vue({
  router,
  render: h => h(App),
}).$mount('#app')
