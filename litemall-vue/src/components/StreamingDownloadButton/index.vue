<template>
  <div class="download-app-section">
    <button class="streaming-download-btn" @click="handleDownload">
      <span class="btn-content">
        <svg width="24" height="24" fill="currentColor" viewBox="0 0 24 24" class="download-icon">
          <path d="M5,20H19V18H5M19,9H15V3H9V9H5L12,16L19,9Z"/>
        </svg>
        {{ buttonText }}
      </span>
      <div class="streaming-effect"></div>
    </button>
  </div>
</template>

<script>
export default {
  name: 'StreamingDownloadButton',
  
  props: {
    buttonText: {
      type: String,
      default: '点击下载金软助手App'
    },
    downloadUrl: {
      type: String,
      default: '/app-download.html'
    },
    openInNewTab: {
      type: Boolean,
      default: true
    }
  },
  
  methods: {
    handleDownload() {
      this.$emit('download-click');
      
      if (this.openInNewTab) {
        window.open(this.downloadUrl, '_blank');
      } else {
        window.location.href = this.downloadUrl;
      }
    }
  }
};
</script>

<style scoped lang="scss">
.download-app-section {
  display: flex;
  justify-content: center;
  margin: 30px 0;
  padding: 0 16px;
}

.streaming-download-btn {
  position: relative;
  padding: 16px 32px;
  border: none;
  border-radius: 50px;
  font-size: 16px;
  font-weight: 600;
  color: #000;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  //box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  
  // 渐变背景：使用app-download.html中4个feature-icon的颜色
  background: linear-gradient(
    45deg,
    #ff6b6b 0%,      // 医疗器械库管理 - 温暖橙红
    #4ecdc4 25%,     // UDI扫码录入 - 清新蓝绿  
    #a8edea 50%,     // 数据安全可靠 - 温暖紫粉
    #ffeaa7 75%,     // 操作简单高效 - 阳光黄橙
    #ff6b6b 100%     // 回到起始色
  );
  background-size: 300% 300%;
  animation: gradientShift 3s ease infinite;

  &:hover {
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 12px 35px rgba(0, 0, 0, 0.3);
    
    .streaming-effect {
      animation-play-state: running;
    }
  }

  &:active {
    transform: translateY(0) scale(0.98);
  }

  .btn-content {
    position: relative;
    z-index: 2;
    display: flex;
    align-items: center;
    gap: 8px;
    
    .download-icon {
      animation: downloadBounce 2s infinite ease-in-out;
    }
  }

  .streaming-effect {
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(
      90deg,
      transparent,
      rgba(255, 255, 255, 0.4),
      transparent
    );
    animation: streamingFlow 2s infinite linear;
    animation-play-state: paused;
  }
}

// 渐变背景动画
@keyframes gradientShift {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

// 流光效果动画
@keyframes streamingFlow {
  0% {
    left: -100%;
    opacity: 0;
  }
  50% {
    opacity: 1;
  }
  100% {
    left: 100%;
    opacity: 0;
  }
}

// 下载图标跳动动画
@keyframes downloadBounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-3px);
  }
  60% {
    transform: translateY(-1px);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .streaming-download-btn {
    padding: 14px 28px;
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .download-app-section {
    margin: 20px 0;
    padding: 0 12px;
  }
  
  .streaming-download-btn {
    padding: 12px 24px;
    font-size: 13px;
    
    .btn-content {
      gap: 6px;
    }
    
    .download-icon {
      width: 20px;
      height: 20px;
    }
  }
}
</style>