<template>
  <div class="app-container">

    <!-- 查询和其他操作 -->
    <div class="filter-container">
      <el-input
        v-model="listQuery.rootOrderId"
        clearable
        class="filter-item"
        style="width: 150px;"
        :placeholder="$t('mall_order.table.root_order_id')"
        type="number"
      />
      <el-input
        v-model="listQuery.orderId"
        clearable
        class="filter-item"
        style="width: 150px;"
        :placeholder="$t('mall_order.placeholder.filter_order_id')"
        type="number"
      />
      <el-input
        v-model="listQuery.nickname"
        clearable
        class="filter-item"
        style="width: 160px;"
        :placeholder="$t('mall_order.placeholder.filter_nickname')"
      />
      <el-input
        v-model="listQuery.consignee"
        clearable
        class="filter-item"
        style="width: 160px;"
        :placeholder="$t('mall_order.placeholder.filter_consignee')"
      />
      <el-input
        v-model="listQuery.orderSn"
        clearable
        class="filter-item"
        style="width: 160px;"
        :placeholder="$t('mall_order.placeholder.filter_order_sn')"
      />
      <el-date-picker
        v-model="listQuery.timeArray"
        type="datetimerange"
        value-format="yyyy-MM-dd HH:mm:ss"
        class="filter-item"
        :range-separator="$t('mall_order.text.date_range_separator')"
        :start-placeholder="$t('mall_order.placeholder.filter_time_start')"
        :end-placeholder="$t('mall_order.placeholder.filter_time_end')"
        :picker-options="pickerOptions"
      />
      <el-select
        v-model="listQuery.orderStatusArray"
        multiple
        style="width: 200px"
        class="filter-item"
        :placeholder="$t('mall_order.placeholder.filter_order_status')"
      >
        <el-option v-for="(key, value) in statusMap" :key="key" :label="key" :value="value" />
      </el-select>
      <el-button
        v-permission="['GET /admin/order/list']"
        class="filter-item"
        type="primary"
        icon="el-icon-search"
        @click="handleFilter"
      >{{ $t('app.button.search') }}</el-button>
      <el-button
        :loading="downloadLoading"
        class="filter-item"
        type="primary"
        icon="el-icon-download"
        @click="handleDownload"
      >{{ $t('app.button.download') }}</el-button>
    </div>

    <!-- 查询结果 -->
    <el-table
      v-loading="listLoading"
      :data="list"
      :element-loading-text="$t('app.message.list_loading')"
      border
      fit
      highlight-current-row
    >

      <el-table-column type="expand">
        <template slot-scope="props">
          <div v-for="item in props.row.goodsVoList" :key="item.id" class="order-goods">
            <div class="picture">
              <img :src="item.picUrl" width="40">
            </div>
            <div class="name">
              {{ $t('mall_order.text.expand_goods_name', { goods_name: item.goodsName }) }}
            </div>
            <div class="spec">
              {{ $t('mall_order.text.expand_specifications', {
                specifications:
                  item.specifications.join('-') }) }}
            </div>
            <div class="price">
              {{ $t('mall_order.text.expand_unit_price', { price: item.price }) }}
            </div>
            <div class="num">
              {{ $t('mall_order.text.expand_number', { number: item.number }) }}
            </div>
            <div class="price">
              {{ $t('mall_order.text.expand_subtotal_price', { price: item.price * item.number }) }}
            </div>
            <div class="other-att">
              {{ $t('mall_order.text.expand_serial', { serial: item.serial }) }}
            </div>
            <div class="other-att">
              {{ $t('mall_order.text.expand_bound_serial', { bound_serial: item.boundSerial }) }}
            </div>
            <div class="other-att">
              {{ $t('mall_order.text.expand_max_client_count', { max_client_count: item.maxClientsCount })
              }}
            </div>
            <div class="other-att">
              {{ $t('mall_order.text.expand_max_register_user_count', {
                max_register_user_count:
                  item.maxRegisterUsersCount }) }}
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column
        align="center"
        min-width="50"
        max-width="100"
        :label="$t('mall_order.table.root_order_id')"
        prop="rootOrderId"
      />
      <el-table-column
        align="center"
        min-width="50"
        max-width="100"
        :label="$t('mall_order.table.parent_order_id')"
        prop="parentOrderId"
      />
      <el-table-column
        align="center"
        min-width="50"
        max-width="100"
        :label="$t('mall_order.table.order_id')"
        prop="id"
      />
      <el-table-column align="center" min-width="120" :label="$t('mall_order.table.order_sn')" prop="orderSn" />

      <el-table-column align="center" :label="$t('mall_order.table.avatar')" width="80">
        <template slot-scope="scope">
          <el-avatar :src="scope.row.avatar" />
        </template>
      </el-table-column>

      <!-- <el-table-column align="center" :label="$t('mall_order.table.trader_id')" prop="traderId" /> -->
      <el-table-column align="center" :label="$t('mall_order.table.trader_info')" prop="traderName">
        <template slot-scope="scope">
          <el-tooltip class="item" effect="dark" :content="scope.row.traderName" placement="top-start">
            <el-link> {{ scope.row.traderName.split(',')[1] }} </el-link>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column align="center" :label="$t('mall_order.table.user_name')" prop="userName" />

      <el-table-column align="center" :label="$t('mall_order.table.add_time')" prop="addTime" min-width="100">
        <template slot-scope="scope">
          <!-- {{ (scope.row.addTime || '').substring(0, 10) }} -->
          {{ scope.row.addTime }}
        </template>
      </el-table-column>
      <el-table-column align="center" :label="$t('mall_order.table.order_status')" prop="orderStatus">
        <template slot-scope="scope">
          <el-tag>{{ scope.row.orderStatus | orderStatusFilter }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column align="center" :label="$t('mall_order.table.pay_type')" prop="payTypeName">
        <template slot-scope="scope">
          <el-tag>{{ scope.row.payTypeName }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column align="center" :label="$t('mall_order.table.order_price')" prop="orderPrice">
        <template slot-scope="scope">
          {{ scope.row.orderPrice }} 元
        </template>
      </el-table-column>

      <el-table-column align="center" :label="$t('mall_order.table.actual_price')" prop="actualPrice">
        <template slot-scope="scope">
          {{ scope.row.actualPrice }} 元
        </template>
      </el-table-column>

      <el-table-column align="center" :label="$t('mall_order.table.pay_time')" prop="payTime" />

      <el-table-column align="center" :label="$t('mall_order.table.consignee')" prop="consignee">
        <template slot-scope="scope">
          <span style="color:red; font-weight:bold;">{{ scope.row.consignee }}</span>
        </template>
      </el-table-column>

      <el-table-column align="center" :label="$t('mall_order.table.mobile')" prop="mobile" min-width="100" />

      <el-table-column align="center" :label="$t('mall_order.table.ship_sn')" prop="shipSn" />

      <el-table-column align="center" :label="$t('mall_order.table.ship_channel')" prop="shipChannel" />

      <!-- <el-table-column align="center" :label="发票" prop="invoiceUrl" /> -->

      <el-table-column align="center" :label="$t('mall_order.table.actions')" width="90" class-name="oper">
        <template slot-scope="scope">
          <!-- <el-button type="primary" size="mini" @click="handleDetail(scope.row)">{{ $t('app.button.detail') }}</el-button>
          <el-button type="danger" size="mini" @click="handleDelete(scope.row)">{{ $t('app.button.delete') }}</el-button>
          <el-button type="warning" size="mini" @click="handlePay(scope.row)">{{ $t('mall_order.button.pay') }}</el-button>
          <el-tooltip class="item" effect="dark" content="发货及更新商品的序列号等参数" placement="top-start">
            <el-button type="primary" size="mini" @click="handleShip(scope.row)">{{ $t('mall_order.button.ship') }}</el-button>
          </el-tooltip>
          <el-button type="danger" size="mini" @click="handleRefund(scope.row)">{{ $t('mall_order.button.refund') }}</el-button> -->
          <el-dropdown @command="executeTableAction">
            <el-button type="primary" style="width: 60px;">操作<i
              class="el-icon-arrow-down el-icon--right"
            />
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item
                v-for="(item, index) in tableActionOptions"
                :key="index"
                :icon="item.icon"
                :command="handleActionCommand(scope.row, item.value, item.param)"
              >
                {{ renderDropdownMenuLabel(scope.row, item) }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="listQuery.page"
      :limit.sync="listQuery.limit"
      @pagination="getList"
    />

    <!-- 订单详情对话框 -->
    <el-dialog :visible.sync="orderDialogVisible" :title="$t('mall_order.dialog.detail', {orderId: orderDetail.order.id})" width="800">
      <section ref="print">
        <el-form :data="orderDetail" label-position="left">
          <el-form-item :label="$t('mall_order.form.detail_order_id')">
            <span>{{ orderDetail.order.id }}</span>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_order_sn')">
            <span>{{ orderDetail.order.orderSn }}</span>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_order_status')">
            <el-tag>{{ orderDetail.order.orderStatus | orderStatusFilter }}</el-tag>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_order_trader_id')">
            <span>{{ orderDetail.order.traderId }}</span>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_order_trader_name')">
            <span>{{ orderDetail.order.traderName }}</span>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_user_nickname')">
            <span>{{ orderDetail.user.nickname }}</span>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_message')">
            <span>{{ orderDetail.order.message }}</span>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_receiving_info')">
            <span>{{ $t('mall_order.text.detail_consigne', { consignee: orderDetail.order.consignee })
            }}</span>
            <span>{{ $t('mall_order.text.detail_mobile', { mobile: orderDetail.order.mobile }) }}</span>
            <span>{{ $t('mall_order.text.detail_address', { address: orderDetail.order.address }) }}</span>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_goods')">
            <el-table :data="orderDetail.orderGoods" border fit highlight-current-row>
              <el-table-column
                align="center"
                :label="$t('mall_order.table.detail_goods_name')"
                prop="goodsName"
              />
              <el-table-column
                align="center"
                :label="$t('mall_order.table.detail_goods_sn')"
                prop="goodsSn"
              />
              <el-table-column
                align="center"
                :label="$t('mall_order.table.detail_goods_specifications')"
                prop="specifications"
              />
              <el-table-column
                align="center"
                :label="$t('mall_order.table.detail_goods_price')"
                prop="price"
              />
              <el-table-column
                align="center"
                :label="$t('mall_order.table.detail_goods_number')"
                prop="number"
              />
              <el-table-column
                align="center"
                :label="$t('mall_order.table.detail_goods_number')"
                prop="maxClientsCount"
              />
              <el-table-column align="center" :label="$t('mall_order.table.goods_attis')" width="150">
                <template slot-scope="scope">
                  <div style="text-align: left;">
                    <p>{{ $t('mall_order.text.expand_serial', { serial: scope.row.serial }) }}</p>
                    <p>{{ $t('mall_order.text.expand_bound_serial', {
                      bound_serial:
                        scope.row.boundSerial }) }}</p>
                    <p>{{ $t('mall_order.text.expand_max_client_count', {
                      max_client_count:
                        scope.row.maxClientsCount }) }}</p>
                    <p>{{ $t('mall_order.text.expand_max_register_user_count', {
                      max_register_user_count: scope.row.maxRegisterUsersCount }) }}</p>
                  </div>
                </template>
              </el-table-column>
              <el-table-column
                align="center"
                :label="$t('mall_order.table.detail_goods_pic_url')"
                prop="picUrl"
              >
                <template slot-scope="scope">
                  <img :src="scope.row.picUrl" width="40">
                </template>
              </el-table-column>
            </el-table>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_price_info')">
            <span>
              {{ $t('mall_order.text.detail_price_info', {
                actual_price: orderDetail.order.actualPrice,
                goods_price: orderDetail.order.goodsPrice,
                freight_price: orderDetail.order.freightPrice,
                coupon_price: orderDetail.order.couponPrice,
                integral_price: orderDetail.order.integralPrice
              }) }}
            </span>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_pay_info')">
            <span>{{ $t('mall_order.text.detail_pay_channel', { pay_channel: orderDetail.order.payTypeName }) }}</span>
            <span>{{ $t('mall_order.text.detail_pay_time', { pay_time: orderDetail.order.payTime })
            }}</span>
            <span> {{ $t('mall_order.text.detail_pay_voucher_url') }}</span>
            <span><a style="color: #409EFF" href="#" @click="openImageInNewTab(orderDetail.order.payVoucherUrl)">{{ orderDetail.order.payVoucherUrl == '' ? '无' : '点击查看' }}</a></span>
            <span> {{ $t('mall_order.text.detail_invoice_url') }}</span>
            <span><a style="color: #409EFF" href="#" @click="openImageInNewTab(orderDetail.order.invoiceUrl)">{{ orderDetail.order.invoiceUrl == '' ? '无' : '点击查看' }}</a></span>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_ship_info')">
            <span>{{ $t('mall_order.text.detail_ship_channel', {
              ship_channel: orderDetail.order.shipChannel
            })
            }}</span>
            <span>{{ $t('mall_order.text.detail_ship_sn', { ship_sn: orderDetail.order.shipSn }) }}</span>
            <span>{{ $t('mall_order.text.detail_ship_time', { ship_time: orderDetail.order.shipTime })
            }}</span>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_refund_info')">
            <span>{{ $t('mall_order.text.detail_refund_amount', {
              refund_amount:
                orderDetail.order.refundAmount })
            }}</span>
            <span>{{ $t('mall_order.text.detail_refund_type', { refund_type: orderDetail.order.refundType })
            }}</span>
            <span>{{ $t('mall_order.text.detail_refund_content', {
              refund_content:
                orderDetail.order.refundContent
            }) }}</span>
            <span>{{ $t('mall_order.text.detail_refund_time', { refund_time: orderDetail.order.refundTime })
            }}</span>
          </el-form-item>
          <el-form-item :label="$t('mall_order.form.detail_receipt_info')">
            <span>{{ $t('mall_order.text.detail_confirm_time', {
              confirm_time: orderDetail.order.confirmTime
            })
            }}</span>
          </el-form-item>
        </el-form>
      </section>
      <span slot="footer" class="dialog-footer">
        <el-button @click="orderDialogVisible = false">{{ $t('mall_order.button.detail_cancel') }}</el-button>
        <el-button type="primary" @click="printOrder">{{ $t('mall_order.button.detail_print') }}</el-button>
      </span>
    </el-dialog>

    <!-- 收款对话框 -->
    <el-dialog :visible.sync="payDialogVisible" :title="$t('mall_order.dialog.pay', {orderId: payForm.orderId})" width="40%">
      <el-form ref="payForm" :model="payForm" status-icon label-position="left" label-width="100px">
        <div style="margin-bottom: 10px;">
          {{ $t('mall_order.message.pay_confirm', { order_sn: payForm.orderSn }) }}
        </div>
        <el-form-item :label="$t('mall_order.form.pay_old_money')" prop="oldMoney">
          <el-input-number v-model="payForm.oldMoney" :controls="false" disabled />
        </el-form-item>
        <el-form-item :label="$t('mall_order.form.pay_new_money')" prop="newMoney">
          <el-input-number v-model="payForm.newMoney" :controls="false" />
        </el-form-item>
      </el-form>
      <el-table :data="payForm.goodsList">
        <el-table-column property="goodsName" :label="$t('mall_order.table.pay_goods_name')" />
        <el-table-column :label="$t('mall_order.table.pay_goods_specifications')">
          <template slot-scope="scope">
            {{ scope.row.specifications.join('-') }}
          </template>
        </el-table-column>
        <el-table-column property="onumber" width="100" :label="$t('mall_order.table.pay_goods_number')" />
        <!-- <el-table-column label="实际数量" width="100">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.number" :min="0" :controls="false" />
          </template>
        </el-table-column> -->
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="payDialogVisible = false">{{ $t('app.button.cancel') }}</el-button>
        <el-button type="primary" @click="confirmPay">{{ $t('app.button.confirm') }}</el-button>
      </div>
    </el-dialog>

    <!-- 发货对话框 -->
    <el-dialog :visible.sync="shipDialogVisible" :title="$t('mall_order.dialog.ship', {orderId: shipForm.orderId})">
      <el-form
        ref="shipForm"
        :model="shipForm"
        status-icon
        label-position="left"
        label-width="100px"
        style="width: 400px; margin-left:50px;"
      >
        <el-form-item :label="$t('mall_order.form.ship_channel')" prop="shipChannel">
          <el-select v-model="shipForm.shipChannel" :placeholder="$t('mall_order.placeholder.ship_channel')">
            <el-option v-for="item in channels" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('mall_order.form.ship_sn')" prop="shipSn">
          <el-input v-model="shipForm.shipSn" />
        </el-form-item>
        <el-form-item :label="$t('mall_order.form.detail_parent_order_id')" prop="parentOrderId">
          <!-- <el-input v-model="shipForm.parentOrderId" /> 为什么不能编辑？ -->
          <input
            v-model="shipForm.parentOrderId"
            type="number"
            class="el-input__inner"
            :placeholder="$t('mall_order.placeholder.parent_order_id')"
          >
        </el-form-item>
      </el-form>
      <el-table :data="shipForm.goodsList">
        <el-table-column property="goodsName" :label="$t('mall_order.table.pay_goods_name')" />
        <el-table-column :label="$t('mall_order.table.pay_goods_specifications')">
          <template slot-scope="scope">
            {{ scope.row.specifications.join('-') }}
          </template>
        </el-table-column>
        <el-table-column property="onumber" width="100" :label="$t('mall_order.table.pay_goods_number')" />
        <el-table-column property="serial" width="130" :label="$t('mall_order.table.goods_serial_number')">
          <template slot-scope="scope">
            <!-- el-input不能编辑，BUG？ -->
            <!-- <el-input v-model="scope.row.serial" :placeholder="$t('mall_order.placeholder.goods_serial_number')" /> -->
            <input
              type="text"
              :value="scope.row.serial"
              class="el-input__inner"
              :placeholder="$t('mall_order.placeholder.goods_serial_number')"
              @input="handleGoodsAttValue($event, scope.$index, 'serial')"
            >
          </template>
        </el-table-column>
        <el-table-column property="boundSerial" width="130" :label="$t('mall_order.table.goods_bound_serial')">
          <template slot-scope="scope">
            <input
              type="text"
              :value="scope.row.boundSerial"
              class="el-input__inner"
              :placeholder="$t('mall_order.placeholder.goods_bound_serial')"
              @input="handleGoodsAttValue($event, scope.$index, 'boundSerial')"
            >
          </template>
        </el-table-column>
        <el-table-column
          property="maxClientsCount"
          width="80"
          :label="$t('mall_order.table.goods_max_client_count')"
        >
          <template slot-scope="scope">
            <span class="form-num">
              <input
                type="number"
                :value="scope.row.maxClientsCount"
                class="el-input__inner"
                :placeholder="$t('mall_order.placeholder.goods_max_client_count')"
                @input="handleGoodsAttValue($event, scope.$index, 'maxClientsCount')"
              >
            </span>
          </template>
        </el-table-column>
        <el-table-column
          property="maxRegisterUsersCount"
          width="80"
          :label="$t('mall_order.table.goods_max_register_user_count')"
        >
          <template slot-scope="scope">
            <input
              type="number"
              :value="scope.row.maxRegisterUsersCount"
              class="el-input__inner"
              :placeholder="$t('mall_order.placeholder.goods_max_register_user_count')"
              @input="handleGoodsAttValue($event, scope.$index, 'maxRegisterUsersCount')"
            >
          </template>
        </el-table-column>
        <!-- <el-table-column label="实际数量" width="100">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.number" :min="0" :controls="false" />
          </template>
        </el-table-column> -->
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="shipDialogVisible = false">{{ $t('app.button.cancel') }}</el-button>
        <el-button type="primary" @click="confirmShip">{{ $t('app.button.confirm') }}</el-button>
      </div>
    </el-dialog>

    <!-- 退款对话框 -->
    <el-dialog :visible.sync="refundDialogVisible" :title="$t('mall_order.dialog.refund', {orderId: refundForm.orderId})">
      <el-form
        ref="refundForm"
        :model="refundForm"
        status-icon
        label-position="left"
        label-width="100px"
        style="width: 400px; margin-left:50px;"
      >
        <el-form-item :label="$t('mall_order.form.refund_money')" prop="refundMoney">
          <el-input v-model="refundForm.refundMoney" :disabled="true" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="refundDialogVisible = false">{{ $t('app.button.cancel') }}</el-button>
        <el-button type="primary" @click="confirmRefund">{{ $t('app.button.confirm') }}</el-button>
      </div>
    </el-dialog>

    <!-- 上传发票对话框 -->
    <el-dialog :visible.sync="uploadInvoiceDialogVisible" :title="$t('mall_order.dialog.upload_invoice', {orderId: uploadInvoiceForm.orderId})">
      <el-form
        ref="uploadInvoiceForm"
        :model="uploadInvoiceForm"
        status-icon
        label-position="left"
        label-width="100px"
        style="width: 400px; margin-left:50px;"
      >
        <el-form-item :label="$t('mall_order.form.pay_new_money')" prop="newMoney">
          <el-input v-model="uploadInvoiceForm.newMoney" :disabled="true" />
        </el-form-item>

        <el-form-item :label="$t('mall_order.form.invoice_url')" prop="invoiceUrl">
          <el-upload
            :headers="headers"
            :action="uploadPath"
            :show-file-list="false"
            :on-success="uploadPicUrl"
            class="avatar-uploader"
            accept=".jpg,.jpeg,.png,.gif,.pdf"
          >
            <img v-if="uploadInvoiceForm.invoiceUrl && uploadInvoiceForm.invoiceUrl.indexOf('.pdf') === -1" :src="uploadInvoiceForm.invoiceUrl" class="avatar" width="100" height="100">
            <embed
              v-else-if="uploadInvoiceForm.invoiceUrl && uploadInvoiceForm.invoiceUrl.indexOf('.pdf') > -1"
              type="application/pdf"
              :src="uploadInvoiceForm.invoiceUrl + '#page=1&view=fitH,100'"
              width="50"
              height="50"
              style="border: 1px solid #ccc;"
            >
            <div class="upload_container">
              <i class="el-icon-plus avatar-uploader-icon" />
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="uploadInvoiceDialogVisible = false">{{ $t('app.button.cancel') }}</el-button>
        <el-button type="primary" @click="confirmUploadInvoice">{{ $t('app.button.confirm') }}</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<style lang="scss" scoped>
.el-dropdown-link {
    cursor: pointer;
    color: #409EFF;
}

.el-icon-arrow-down {
    font-size: 12px;
}

.el-table--medium th,
.el-table--medium td {
    padding: 3px 0;
}

.el-input-number--medium {
    width: 100%;
}

.oper .el-button--mini {
    padding: 7px 4px;
    width: 40px;
    font-size: 10px;
    margin-left: 1px;
}

::v-deep .el-table__expanded-cell {
    padding: 6px 80px;
}

.order-goods {
    display: flex;
    justify-content: space-around;
    justify-items: center;
    align-items: center;
    padding: 6px 0;
}

.name {
    width: 400px;
}

.spec {
    width: 180px;
}

.price {
    width: 120px;
}

.num {
    width: 120px;
}

.other-att {
    width: 180px;
    font-weight: bold;
}

.el-form-item span {
    border-bottom: 1px solid #bababb;
}

.upload_container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  border: 1px dashed #ccc;
  border-radius: 4px;
}
</style>

<script>
import { detailOrder, listOrder, listChannel, refundOrder, payOrder, deleteOrder, shipOrder, uploadInvoice } from '@/api/order'
import Pagination from '@/components/Pagination' // Secondary package based on el-pagination
import checkPermission from '@/utils/permission' // 权限判断函数
import { uploadPath } from '@/api/storage'
import { getToken } from '@/utils/auth'

const statusMap = {
  101: '未付款',
  102: '用户取消',
  103: '系统取消',
  201: '已付款',
  202: '申请退款',
  203: '已退款',
  301: '已发货',
  401: '用户收货',
  402: '系统收货'
}

const actionOptions = [{
  value: '1', // 下拉菜单唯一标识
  label: '详情', // 菜单默认操作名称
  icon: 'el-icon-more', // 菜单图标
  param: 'look' // 其他参数（不同于表格所需的scope.row及value），如无传空
}, {
  value: '2',
  label: '删除',
  icon: 'el-icon-delete',
  param: 'change'
}, {
  value: '3',
  label: '收款',
  icon: 'el-icon-money',
  param: ''
}, {
  value: '4',
  label: '发货',
  icon: 'el-icon-ship',
  param: ''
}, {
  value: '5',
  label: '退款',
  icon: 'el-icon-sunrise',
  param: ''
}, {
  value: '6',
  label: '上传发票',
  icon: 'el-icon-sunrise',
  param: ''
}]

export default {
  name: 'Order',
  components: { Pagination },
  filters: {
    orderStatusFilter(status) {
      return statusMap[status]
    }
  },
  data() {
    return {
      tableActionOptions: actionOptions,
      list: [],
      total: 0,
      listLoading: true,
      listQuery: {
        page: 1,
        limit: 20,
        nickname: undefined,
        consignee: undefined,
        orderSn: undefined,
        timeArray: [],
        orderStatusArray: [],
        sort: 'add_time',
        order: 'desc'
      },
      pickerOptions: {
        shortcuts: [{
          text: '最近一周',
          onClick(picker) {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
            picker.$emit('pick', [start, end])
          }
        }, {
          text: '最近一个月',
          onClick(picker) {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
            picker.$emit('pick', [start, end])
          }
        }, {
          text: '最近三个月',
          onClick(picker) {
            const end = new Date()
            const start = new Date()
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
            picker.$emit('pick', [start, end])
          }
        }]
      },
      statusMap,
      orderDialogVisible: false,
      orderDetail: {
        order: {},
        user: {},
        orderGoods: []
      },
      shipForm: {
        orderId: undefined,
        shipChannel: undefined,
        shipSn: undefined
      },
      shipDialogVisible: false,
      payForm: {
        orderId: undefined,
        orderSn: '',
        oldMoney: 0,
        newMoney: 0,
        goodsList: []
      },
      payDialogVisible: false,
      refundForm: {
        orderId: undefined,
        refundMoney: undefined
      },
      refundDialogVisible: false,
      uploadInvoiceForm: {
        orderId: undefined,
        invoiceUrl: undefined,
        newMoney: undefined
      },
      uploadInvoiceDialogVisible: false,
      uploadPath,
      downloadLoading: false,
      channels: []
    }
  },
  computed: {
    headers() {
      return {
        'X-Litemall-Admin-Token': getToken()
      }
    }
  },
  created() {
    this.getList()
    this.getChannel()
  },
  methods: {
    checkPermission,
    getList() {
      this.listLoading = true
      if (this.listQuery.timeArray && this.listQuery.timeArray.length === 2) {
        this.listQuery.start = this.listQuery.timeArray[0]
        this.listQuery.end = this.listQuery.timeArray[1]
      } else {
        this.listQuery.start = null
        this.listQuery.end = null
      }
      if (this.listQuery.orderId) {
        detailOrder(this.listQuery.orderId).then(response => {
          this.list = []
          if (response.data.data.order) {
            // 无论是通过列表查询还是订单ID单独查询，返回的数据结构都是一致的，展开行都能正常显示商品信息
            response.data.data.order.goodsVoList = response.data.data.orderGoods || []
            this.list.push(response.data.data.order)
            this.total = 1
            this.listLoading = false
          }
        }).catch(() => {
          this.list = []
          this.total = 0
          this.listLoading = false
        })
      } else {
        listOrder(this.listQuery).then(response => {
          this.list = response.data.data.list
          this.total = response.data.data.total
          this.listLoading = false
        }).catch(() => {
          this.list = []
          this.total = 0
          this.listLoading = false
        })
      }
    },
    // 表格菜单操作
    executeTableAction(c) {
      switch (c.command) {
        case '1':
          // this.handleDetail(c.row, c.param)
          this.handleDetail(c.row)
          break
        case '2':
          this.handleDelete(c.row)
          break
        case '3':
          this.handlePay(c.row)
          break
        case '4':
          this.handleShip(c.row)
          break
        case '5':
          this.handleRefund(c.row)
          break
        case '6':
          this.handleUploadInvoice(c.row)
          break
        default:
          return
      }
    },
    // 将command属性封装成一个对象 便于@command事件可获得多个参数
    handleActionCommand(row, command, param) {
      return {
        row: row,
        command: command,
        param: param
      }
    },
    // 默认返回label, 可根据表格属性自定义返回内容（采用闭包传参）
    renderDropdownMenuLabel: function(row, item) {
      if (item.value === '1') {
        // return row.status === 1 ? '开启' : '关闭'
      }
      return item.label
    },
    getChannel() {
      listChannel().then(response => {
        this.channels = response.data.data
      })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    handleDetail(row) {
      detailOrder(row.id).then(response => {
        this.orderDetail = response.data.data
      })
      this.orderDialogVisible = true
    },
    handlePay(row) {
      this.payForm.orderId = row.id
      this.payForm.orderSn = row.orderSn
      this.payForm.oldMoney = row.actualPrice
      this.payForm.newMoney = row.actualPrice
      this.payForm.goodsList = row.goodsVoList
      this.payForm.goodsList.forEach(element => {
        element.onumber = element.number
      })
      this.payDialogVisible = true
    },
    confirmPay() {
      if (this.payForm.oldMoney !== this.payForm.newMoney) {
        const diff = this.payForm.newMoney - this.payForm.oldMoney
        this.$confirm('差额 ' + diff + '元， 是否确认提交')
          .then(_ => {
            this.confirmPay2()
          })
          .catch(_ => { })
      } else {
        this.confirmPay2()
      }
    },
    confirmPay2() {
      payOrder(this.payForm).then(response => {
        this.$notify.success({
          title: '成功',
          message: '订单收款操作成功'
        })
        this.getList()
      }).catch(response => {
        this.$notify.error({
          title: '失败',
          message: response.data.errmsg
        })
      }).finally(() => {
        this.payDialogVisible = false
      })
    },
    handleShip(row) {
      this.shipForm.orderId = row.id
      this.shipForm.parentOrderId = row.parentOrderId
      this.shipForm.shipChannel = row.shipChannel
      this.shipForm.shipSn = row.shipSn
      this.shipForm.goodsInputList = []
      this.shipForm.goodsList = row.goodsVoList
      this.shipForm.goodsList.forEach(element => {
        element.onumber = element.number
        // 添加订单明细ID及对应的序列号等参数
        this.shipForm.goodsInputList.push({
          detId: element.id,
          serial: element.serial,
          boundSerial: element.boundSerial,
          maxClientsCount: element.maxClientsCount,
          maxRegisterUsersCount: element.maxRegisterUsersCount
        })
      })

      this.shipDialogVisible = true
      this.$nextTick(() => {
        this.$refs['shipForm'].clearValidate()
      })
    },
    // 更新订单明细中的商品参数
    handleGoodsAttValue(e, rowIndex, attributeName) {
      onEditOrderGoodsAtt(e, this.list, this.shipForm.goodsInputList, rowIndex, attributeName)
    },
    confirmShip() {
      this.$refs['shipForm'].validate((valid) => {
        if (valid) {
          const backupGoodsList = this.shipForm.goodsList
          delete this.shipForm.goodsList
          shipOrder(this.shipForm).then(response => {
            this.shipForm.goodsList = backupGoodsList
            this.shipDialogVisible = false
            this.$notify.success({
              title: '成功',
              message: '确认发货成功'
            })
            this.getList()
          }).catch(response => {
            this.shipForm.goodsList = backupGoodsList
            if (response.data.errno === 620) {
              // 更新订单明细中的商品参数成功
              this.shipDialogVisible = false
              this.$notify.success({
                title: '成功',
                message: '更新商品序列号等参数成功'
              })
              this.getList()
            } else {
              this.$notify.error({
                title: '失败',
                message: response.data.errmsg
              })
            }
          })
        }
      })
    },
    handleDelete(row) {
      deleteOrder({ orderId: row.id }).then(response => {
        this.$notify.success({
          title: '成功',
          message: '订单删除成功'
        })
        this.getList()
      }).catch(response => {
        this.$notify.error({
          title: '失败',
          message: response.data.errmsg
        })
      })
    },
    handleRefund(row) {
      this.refundForm.orderId = row.id
      this.refundForm.refundMoney = row.actualPrice

      this.refundDialogVisible = true
      this.$nextTick(() => {
        this.$refs['refundForm'].clearValidate()
      })
    },
    confirmRefund() {
      this.$refs['refundForm'].validate((valid) => {
        if (valid) {
          refundOrder(this.refundForm).then(response => {
            this.refundDialogVisible = false
            this.$notify.success({
              title: '成功',
              message: '确认退款成功'
            })
            this.getList()
          }).catch(response => {
            this.$notify.error({
              title: '失败',
              message: response.data.errmsg
            })
          })
        }
      })
    },
    handleUploadInvoice(row) {
      this.uploadInvoiceForm.orderId = row.id
      this.uploadInvoiceForm.newMoney = row.actualPrice
      this.uploadInvoiceForm.invoiceUrl = row.invoiceUrl
      this.uploadInvoiceDialogVisible = true
      this.$nextTick(() => {
        this.$refs['uploadInvoiceForm'].clearValidate()
      })
    },
    confirmUploadInvoice() {
      this.$refs['uploadInvoiceForm'].validate((valid) => {
        if (valid) {
          uploadInvoice(this.uploadInvoiceForm).then(response => {
            this.uploadInvoiceDialogVisible = false
            this.$notify.success({
              title: '成功',
              message: '确认上传发票成功'
            })
            this.getList()
          }).catch(response => {
            this.$notify.error({
              title: '失败',
              message: response.data.errmsg
            })
          })
        }
      })
    },
    uploadPicUrl: function(response) {
      this.uploadInvoiceForm.invoiceUrl = response.data.url
    },
    handleDownload() {
      this.downloadLoading = true
      import('@/vendor/Export2Excel').then(excel => {
        const tHeader = ['订单ID', '订单编号', '用户ID', '订单状态', '是否删除', '收货人', '收货联系电话', '收货地址']
        const filterVal = ['id', 'orderSn', 'userId', 'orderStatus', 'isDelete', 'consignee', 'mobile', 'address']
        excel.export_json_to_excel2(tHeader, this.list, filterVal, '订单信息')
        this.downloadLoading = false
      })
    },
    printOrder() {
      this.$print(this.$refs.print)
      this.orderDialogVisible = false
    },
    openImageInNewTab(imageUrl) {
      if (imageUrl) {
        window.open(imageUrl, '_blank')
      }
    }
  }
}

/**
 * 编辑订单商品参数
 * @param e
 * @param rowIndex
 */
function onEditOrderGoodsAtt(e, orderGoodsList, goodsInputList, rowIndex, attributeName) {
  const attributeValue = e.currentTarget.value
  // 编辑的商品参数
  const goodsInputData = goodsInputList[rowIndex]
  goodsInputData[attributeName] = attributeValue
  // 更新订单明细中的商品参数，用于表格的显示
  const orderGoodsData = orderGoodsList[rowIndex]
  orderGoodsData[attributeName] = attributeValue
  // console.log('goodsData', goodsData)
}
</script>
