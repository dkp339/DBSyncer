<template>
  <router-view />
</template>

<script setup>
// 这里暂时不需要写任何逻辑
</script>

<style>
/* 清除浏览器默认的边距，让页面占满全屏 */
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
}
</style>