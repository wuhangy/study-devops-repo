import redis
import time

# 连接 Redis
r = redis.Redis(host='localhost', port=6379, decode_responses=True)

def start_build(project_name):
    # 尝试加锁，有效期 10 秒
    if r.set(f"lock:{project_name}", "busy", nx=True, ex=10):
        print(f"开始执行 {project_name} 的构建...")
        # 模拟工作
        r.hset(f"status:{project_name}", "last_run", time.ctime())
        r.hset(f"status:{project_name}", "result", "Success")
        
        # 任务完成后释放锁
        r.delete(f"lock:{project_name}")
    else:
        print("警告：该项目正在构建中，请勿重复操作！")

start_build("T13T_BEV")