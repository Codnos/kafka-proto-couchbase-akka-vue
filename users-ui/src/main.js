import Vue from 'vue'
import VueRouter from 'vue-router'
import App from './App.vue'
import UserList from './UserList.vue'
import 'semantic-ui-css/semantic.min.css';

Vue.config.productionTip = false
Vue.use(VueRouter)

const Home = { template: '<div><h2>Home</h2></div>' }
const About = { template: '<div><h2>About</h2></div>' }


const routes = [
  { path: '/', component: Home },
  { path: '/about', component: About },
  { path: '/users', component: UserList },
]

const router = new VueRouter({
  routes
})

new Vue({
  router,
  render: h => h(App),
}).$mount('#app')
