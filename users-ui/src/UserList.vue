<template>
    <div id="users">
     <div v-if="loading" class="loading">
          Loading...
        </div>

        <div v-if="error" class="error">
          {{ error }}
        </div>

        <vuetable ref="vuetable"
            :api-mode="false"
            :fields="['name']"
            :data="userList"
          ></vuetable>
    </div>
</template>

<script>
import Vuetable from 'vuetable-2/src/components/Vuetable'

export default {
    name: 'users',
        components: {
            Vuetable
        },
    data() {
      return {
        loading: false,
        userList: null,
        error: "ABC"
      }
    },
    created () {
        this.fetchData()
      },
    methods: {
        fetchData () {
          this.error = this.userList = null
          this.loading = true
          const headers = new Headers({
              'Accept': 'application/json'
          });
          const params = {
            headers: headers
          };
          fetch('/api/users', params)
              .then((response) => {
                if (response.status >= 400) {
                  this.loading = false;
                  response.text().then((txt) => {
                     this.error = "Bad response from server: " + txt;
                  });
                } else {
                  return response.json();
                }
              })
              .then((data) => {
                this.userList = data;
                this.loading = false;
              });
        }
    }
}
</script>

<style>
  .error {
    color: red;
  }
</style>