<template>
  <button
    :class="buttonClass"
    :disabled="disabled"
    :type="type"
    @click="handleClick"
  >
    <slot>{{ text }}</slot>
  </button>
</template>

<script>
export default {
  name: 'CustomButton',
  
  props: {
    // 按钮文本
    text: {
      type: String,
      default: '确定'
    },
    // 按钮类型
    variant: {
      type: String,
      default: 'primary',
      validator: value => ['primary', 'secondary', 'success', 'warning', 'danger'].includes(value)
    },
    // 按钮尺寸
    size: {
      type: String,
      default: 'medium',
      validator: value => ['small', 'medium', 'large'].includes(value)
    },
    // 是否禁用
    disabled: {
      type: Boolean,
      default: false
    },
    // 是否加载中
    loading: {
      type: Boolean,
      default: false
    },
    // 按钮类型属性
    type: {
      type: String,
      default: 'button',
      validator: value => ['button', 'submit', 'reset'].includes(value)
    },
    // 是否为圆角按钮
    round: {
      type: Boolean,
      default: true
    },
    // 是否为块级按钮
    block: {
      type: Boolean,
      default: false
    }
  },
  
  computed: {
    buttonClass() {
      return [
        'custom-btn',
        `custom-btn--${this.variant}`,
        `custom-btn--${this.size}`,
        {
          'custom-btn--disabled': this.disabled || this.loading,
          'custom-btn--loading': this.loading,
          'custom-btn--round': this.round,
          'custom-btn--block': this.block
        }
      ]
    }
  },
  
  methods: {
    handleClick(event) {
      if (this.disabled || this.loading) {
        return
      }
      this.$emit('click', event)
    }
  }
}
</script>

<style scoped>
.custom-btn {
  border: none;
  cursor: pointer;
  transition: all 0.3s;
  outline: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  text-align: center;
  white-space: nowrap;
  user-select: none;
  position: relative;
  overflow: hidden;
}

/* 尺寸 */
.custom-btn--small {
  padding: 8px 16px;
  font-size: 12px;
  min-height: 32px;
}

.custom-btn--medium {
  padding: 12px 20px;
  font-size: 14px;
  min-height: 40px;
}

.custom-btn--large {
  padding: 16px 24px;
  font-size: 16px;
  min-height: 48px;
}

/* 主题色 */
.custom-btn--primary {
  background: linear-gradient(135deg, #029688, #017065);
  color: white;
}

.custom-btn--primary:not(.custom-btn--disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(2, 150, 136, 0.3);
}

.custom-btn--secondary {
  background: #f5f5f5;
  color: #666;
}

.custom-btn--secondary:not(.custom-btn--disabled):hover {
  background: #e8e8e8;
}

.custom-btn--success {
  background: linear-gradient(135deg, #4caf50, #388e3c);
  color: white;
}

.custom-btn--warning {
  background: linear-gradient(135deg, #ff9800, #f57c00);
  color: white;
}

.custom-btn--danger {
  background: linear-gradient(135deg, #f44336, #d32f2f);
  color: white;
}

/* 圆角 */
.custom-btn--round {
  border-radius: 24px;
}

/* 块级 */
.custom-btn--block {
  width: 100%;
  display: flex;
}

/* 禁用状态 */
.custom-btn--disabled {
  background: #e0e0e0 !important;
  color: #999 !important;
  cursor: not-allowed !important;
  transform: none !important;
  box-shadow: none !important;
}

/* 加载状态 */
.custom-btn--loading::before {
  content: '';
  width: 14px;
  height: 14px;
  border: 2px solid transparent;
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 8px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>