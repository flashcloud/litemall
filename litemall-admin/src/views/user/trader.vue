<template>
  <div class="app-container">

    <!-- 查询和其他操作 -->
    <div class="filter-container">
      <el-input v-model="listQuery.name" clearable class="filter-item" style="width: 200px;" :placeholder="$t('user_trader.placeholder.filter_name')" /> &nbsp;&nbsp;
      <el-button v-permission="['GET /admin/trader/list']" class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">{{ $t('app.button.search') }}</el-button>
      <el-button v-permission="['POST /admin/trader/create']" class="filter-item" type="primary" icon="el-icon-edit" @click="handleCreate">{{ $t('app.button.create') }}</el-button>
    </div>

    <!-- 查询结果 -->
    <el-table v-loading="listLoading" :data="list" :element-loading-text="$t('app.message.list_loading')" border fit highlight-current-row>
      <el-table-column align="center" :label="$t('user_trader.table.taxid')" prop="taxid" sortable />
      <el-table-column align="center" :label="$t('user_trader.table.name')" prop="companyName" sortable />
      <el-table-column align="center" :label="$t('user_trader.table.nickname')" prop="nickname" sortable />
      <el-table-column align="center" :label="$t('user_trader.table.address')" prop="address" sortable />
      <el-table-column align="center" :label="$t('user_trader.table.phoneNum')" prop="phoneNum" sortable />

      <el-table-column align="center" :label="$t('user_trader.table.creatorName')" prop="creatorName">
        <template slot-scope="scope">
          <span>{{ formatUsers(scope.row.creatorId, true) }}</span>
        </template>

      </el-table-column>
      <el-table-column align="center" :label="$t('user_trader.table.directorName')" prop="directorName" />

      <el-table-column align="center" :label="$t('user_trader.table.user_ids')" prop="userIds">
        <!-- <template slot-scope="scope">
          <el-link v-for="userId in scope.row.userIds" :key="userId" type="primary" style="margin-right: 20px;"> {{ formatUsers(userId) }} </el-link>
        </template> -->
        <template slot-scope="scope">
          <span v-for="userId in scope.row.userIds" :key="userId" type="primary" style="margin-right: 20px; display: block;"> {{ formatUsers(userId) }} </span>
        </template>
      </el-table-column>

      <el-table-column align="center" :label="$t('common_config.update_time')" prop="updateTime" />

      <el-table-column align="center" :label="$t('user_trader.table.actions')" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button v-permission="['POST /admin/trader/update']" type="primary" size="mini" @click="handleUpdate(scope.row)">{{ $t('app.button.edit') }}</el-button>
          <el-button v-permission="['POST /admin/trader/delete']" type="danger" size="mini" @click="handleDelete(scope.row)">{{ $t('app.button.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="listQuery.page" :limit.sync="listQuery.limit" @pagination="getList" />

    <!-- 添加或修改对话框 -->
    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible">
      <el-form ref="dataForm" :rules="rules" :model="dataForm" status-icon label-position="left" label-width="100px" style="width: 400px; margin-left:50px;">
        <el-form-item :label="$t('user_trader.form.taxid')" prop="taxid">
          <el-input v-model="dataForm.taxid" />
        </el-form-item>
        <el-form-item :label="$t('user_trader.form.name')" prop="companyName">
          <el-input v-model="dataForm.companyName" />
        </el-form-item>
        <el-form-item :label="$t('user_trader.form.nickname')" prop="nickname">
          <el-input v-model="dataForm.nickname" />
        </el-form-item>
        <el-form-item :label="$t('user_trader.form.address')" prop="address">
          <el-input v-model="dataForm.address" />
        </el-form-item>
        <el-form-item :label="$t('user_trader.form.phoneNum')" prop="phoneNum">
          <el-input v-model="dataForm.phoneNum" />
        </el-form-item>
        <el-form-item :label="$t('user_trader.form.directorName')">
          <el-select v-model="dataForm.userId" clearable filterable @change="handleDirectorChange">
            <el-option v-for="item in userDropdownList" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('user_trader.form.desc')" prop="desc">
          <el-input v-model="dataForm.desc" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">{{ $t('app.button.cancel') }}</el-button>
        <el-button v-if="dialogStatus=='create'" type="primary" @click="createData">{{ $t('app.button.confirm') }}</el-button>
        <el-button v-else type="primary" @click="updateData">{{ $t('app.button.confirm') }}</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { listTrader, createTrader, updateTrader, deleteTrader } from '@/api/trader'
import { dropdownList as usersData } from '@/api/user'
import Pagination from '@/components/Pagination'
export default {
  name: 'Trader',
  components: { Pagination },
  data() {
    return {
      list: null,
      total: 0,
      userDropdownList: null,
      listLoading: true,
      listQuery: {
        page: 1,
        limit: 20,
        name: undefined,
        sort: 'add_time',
        order: 'desc'
      },
      dataForm: {
        id: undefined,
        companyName: undefined,
        taxid: undefined,
        address: undefined,
        phoneNum: undefined,
        desc: undefined
      },
      dialogFormVisible: false,
      dialogStatus: '',
      textMap: {
        update: '编辑',
        create: '创建'
      },
      rules: {
        taxid: [
          { required: true, message: '税号不能为空', trigger: 'blur' },
          { min: 18, max: 18, message: '税号长度为18位', trigger: 'blur' }
        ],
        companyName: [
          { required: true, message: '交易企业名称不能为空', trigger: 'blur' }
        ],
        nickname: [
          { required: true, message: '交易企业简称不能为空', trigger: 'blur' }
        ]
      },
      permissionDialogFormVisible: false,
      systemPermissions: null,
      assignedPermissions: null,
      permissionForm: {
        traderId: undefined,
        permissions: []
      }
    }
  },
  created() {
    this.getList()

    usersData()
      .then(response => {
        this.userDropdownList = response.data.data.list
      })
  },
  methods: {
    formatUsers(userId, isAdmin) {
      if (userId === 0) {
        return isAdmin === true ? '管理员' : ''
      }
      for (let i = 0; i < this.userDropdownList.length; i++) {
        if (userId === this.userDropdownList[i].value) {
          return this.userDropdownList[i].label
        }
      }
      return ''
    },
    getList() {
      this.listLoading = true
      listTrader(this.listQuery)
        .then(response => {
          this.list = response.data.data.list
          this.total = response.data.data.total
          this.listLoading = false
        })
        .catch(() => {
          this.list = []
          this.total = 0
          this.listLoading = false
        })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    resetForm() {
      this.dataForm = {
        id: undefined,
        name: undefined,
        desc: undefined
      }
    },
    handleCreate() {
      this.resetForm()
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    createData() {
      this.$refs['dataForm'].validate(valid => {
        if (valid) {
          createTrader(this.dataForm)
            .then(response => {
              this.list.unshift(response.data.data)
              this.dialogFormVisible = false
              this.$notify.success({
                title: '成功',
                message: '添加交易企业成功'
              })
            })
            .catch(response => {
              this.$notify.error({
                title: '失败',
                message: response.data.errmsg
              })
            })
        }
      })
    },
    handleUpdate(row) {
      this.dataForm = Object.assign({}, row)
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    updateData() {
      this.$refs['dataForm'].validate(valid => {
        if (valid) {
          updateTrader(this.dataForm)
            .then(() => {
              for (const v of this.list) {
                if (v.id === this.dataForm.id) {
                  const index = this.list.indexOf(v)
                  this.list.splice(index, 1, this.dataForm)
                  break
                }
              }
              this.dialogFormVisible = false
              this.$notify.success({
                title: '成功',
                message: '更新交易企业成功'
              })
            })
            .catch(response => {
              this.$notify.error({
                title: '失败',
                message: response.data.errmsg
              })
            })
        }
      })
    },
    handleDirectorChange(value) {
      for (let i = 0; i < this.userDropdownList.length; i++) {
        if (value === this.userDropdownList[i].value) {
          this.dataForm.directorName = this.userDropdownList[i].label
          break
        }
      }
    },
    handleDelete(row) {
      this.$confirm('此操作将删除该交易企业, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteTrader(row)
          .then(response => {
            this.$notify.success({
              title: '成功',
              message: '删除交易企业成功'
            })
            this.getList()
          })
          .catch(response => {
            this.$notify.error({
              title: '失败',
              message: response.data.errmsg,
              dangerouslyUseHTMLString: true
            })
          })
        // this.$message({
        //     type: 'success',
        //     message: '删除成功!'
        // })
      }).catch(() => {
        // this.$message({
        //     type: 'info',
        //     message: '已取消删除'
        // })
      })
    }
  }
}
</script>
