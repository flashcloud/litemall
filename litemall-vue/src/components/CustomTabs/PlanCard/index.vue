<template>
  <div 
    class="plan-card" :key="planKey" :ref="`plan-${planId}`" :data-plan-key="planKey"
    :class="[planType, { active: isSelected }]"
    @click="handleClick"
  >
    <!-- 特惠标签 -->
    <div v-if="tag" class="plan-tag">{{ tag }}</div>
    
    <!-- 套餐标题 -->
    <div class="plan-title">{{ title }}</div>
    
    <!-- 价格信息 -->
    <div class="plan-price">
      <span class="current-price">¥{{ currentPrice }}</span>
    </div>
    
    <!-- 原价 -->
    <div v-if="originalPrice" class="original-price">¥{{ originalPrice }}</div>
    
    <!-- 额外说明 -->
    <div v-if="note" class="plan-note">{{ note }}</div>
  </div>
</template>

<script>
export default {
  name: 'PlanCard',
  
  props: {
    // 套餐ID，用于识别选中状态
    planId: {
      type: [Number, String],
      required: true
    },
    goodsId: {
      type: [Number, String],
      required: true
    },    
    planKey: {
      type: String,
      required: true
    },    
    // 套餐标题
    title: {
      type: String,
      required: true
    },
    // 当前价格
    currentPrice: {
      type: [Number, String],
      required: true
    },
    // 原价（可选）
    originalPrice: {
      type: [Number, String],
      default: null
    },
    // 特惠标签（可选）
    tag: {
      type: String,
      default: ''
    },
    // 额外说明（可选）
    note: {
      type: String,
      default: ''
    },
    // 套餐类型，用于应用不同样式
    planType: {
      type: String,
      default: 'default',
      validator: (value) => {
        return ['limited-offer', 'new-friend-offer', 'monthly-card', 'yearly-auto-card', 'default'].includes(value);
      }
    },
    // 是否被选中
    isSelected: {
      type: Boolean,
      default: false
    }
  },
  
  emits: ['select'],
  
  methods: {
    handleClick() {
      this.$emit('select', {
        planId: this.planId,
        goodsId: this.goodsId,
        planKey: this.planKey,
        price: this.currentPrice
      });
    }
  }
};
</script>

<style scoped lang="scss">
.plan-card {
  flex: 1;
  background-color: #fff;
  border-radius: 12px;
  padding: 16px 12px;
  text-align: center;
  cursor: pointer;
  border: 2px solid transparent;
  position: relative;
  transition: all 0.3s ease;
  
  &.active {
    border-color: #8B4513;
    border-width: 2px;
    box-shadow: 0 0 10px rgba(139, 69, 19, 0.3);
  }
  
  &.limited-offer {
    background: linear-gradient(135deg, #ffe5e5 0%, #fff0f0 100%);
  }
  
  &.new-friend-offer {
    background: linear-gradient(135deg, #f5e6a3 0%, #f7e7a4 100%);
    
    .plan-tag {
      background-color: #e6a23c;
      color: white;
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 10px;
      position: absolute;
      top: -6px;
      left: 50%;
      transform: translateX(-50%);
    }
  }
  
  &.monthly-card {
    background: linear-gradient(135deg, #ffe5e5 0%, #fff0f0 100%);
  }

  &.yearly-auto-card {
    background: linear-gradient(135deg, #e6f7ff 0%, #f0faff 100%);

    .plan-tag {
      background-color: #DC143C;
      color: white;
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 10px;
      position: absolute;
      top: -6px;
      left: 50%;
      transform: translateX(-50%);
    }    
  }

  .plan-title {
    font-size: 16px;
    font-weight: bold;
    color: #333;
    margin-bottom: 8px;
    margin-top: 8px;
  }
  
  .plan-price {
    .current-price {
      font-size: 24px;
      font-weight: bold;
      color: #DC143C;
    }
  }
  
  .original-price {
    font-size: 12px;
    color: #999;
    text-decoration: line-through;
    margin-top: 4px;
  }
  
  .plan-note {
    font-size: 11px;
    color: #666;
    margin-top: 4px;
  }
}
</style>