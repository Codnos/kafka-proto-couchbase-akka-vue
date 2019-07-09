<template>
    <div id="users">
     <div v-if="loading" class="loading">
          Loading...
        </div>

        <div v-if="error" class="error">
          {{ error }}
        </div>

        <div v-if="userList" class="content">
          <h2>{{ JSON.stringify(userList) }}</h2>
        </div>
    </div>
</template>

<script>
export default {
    name: 'users',
        components: {
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