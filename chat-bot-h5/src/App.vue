<template>
  <div class="app-wrapper">
    <div id="app-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style lang="scss">
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  background: #f5f5f5;
}

#app {
  width: 100%;
  height: 100%;
}

.app-wrapper {
  width: 100%;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: stretch;
  background: #f0f2f5;

  #app-content {
    width: 100%;
    max-width: 100%;
    min-height: 100vh;
    background: #f5f5f5;
    display: flex;
    flex-direction: column;
  }

  @media (min-width: 768px) {
    #app-content {
      max-width: 480px;
      margin: 0 auto;
      box-shadow: 0 0 40px rgba(0, 0, 0, 0.08);
      background: #f5f5f5;
    }
  }
}
</style>
